package com.example.hearme.ui.screens
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

/**
 * Este composable muestra un botón en forma de micrófono.
 *
 * Al pulsarlo:
 *   - Se solicita el permiso de audio si aún no se tiene.
 *   - Se inicia la grabación (usando MediaRecorder) y se crea un archivo de salida.
 *
 * Durante la grabación se muestra, en lugar del botón,
 * una "barra de grabación" que incluye:
 *   • Un contador con el tiempo (en segundos) que lleva grabando.
 *   • A la derecha, dos iconos: la papelera para cancelar y el icono de enviar para
 *     finalizar la grabación y llamar al callback onAudioRecorded (por ejemplo, para subir a Firebase).
 *
 * @param onAudioRecorded Callback con el archivo de audio grabado (a usar para subirlo)
 * @param onRecordingCanceled Callback para gestionar la cancelación de la grabación.
 */
@Composable
fun AudioRecorderPanel(
    modifier: Modifier = Modifier,
    onAudioRecorded: (File) -> Unit,
    onRecordingCanceled: () -> Unit
) {
    val context = LocalContext.current

    // Verificar permiso para el audio
    var hasAudioPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }
    LaunchedEffect(Unit) {
        hasAudioPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Estados para la grabación
    var isRecording by remember { mutableStateOf(false) }
    var recordingTime by remember { mutableStateOf(0L) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    var recordingJob: Job? by remember { mutableStateOf(null) }
    // Mantenemos la referencia a AudioRecord para controlarlo luego
    var audioRecorder: AudioRecord? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        if (!isRecording) {
            // Botón para iniciar grabación
            FloatingActionButton(
                onClick = {
                    if (hasAudioPermission) {
                        // Creamos el archivo con la extensión .wav
                        val file = File(
                            context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                            "review_audio_${System.currentTimeMillis()}.wav"
                        )
                        outputFile = file

                        // Parámetros para AudioRecord
                        val sampleRate = 16000
                        val channelConfig = AudioFormat.CHANNEL_IN_MONO
                        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                        val bufferSize = if (minBufferSize != AudioRecord.ERROR_BAD_VALUE) minBufferSize else sampleRate * 2

                        // Inicializa AudioRecord
                        val recorder = AudioRecord(
                            MediaRecorder.AudioSource.MIC,
                            sampleRate,
                            channelConfig,
                            audioFormat,
                            bufferSize
                        )
                        audioRecorder = recorder

                        // Inicia grabación
                        recorder.startRecording()
                        isRecording = true
                        recordingTime = 0L

                        // Inicia una coroutine para leer los datos de AudioRecord y escribir al archivo WAV.
                        recordingJob = CoroutineScope(Dispatchers.IO).launch {
                            // Abrimos el archivo y reservamos espacio para el header (44 bytes)
                            file.outputStream().use { outputStream ->
                                // Escribir 44 bytes vacíos, que se actualizarán al detener la grabación.
                                val header = ByteArray(44)
                                outputStream.write(header)

                                val buffer = ByteArray(bufferSize)
                                var totalAudioBytes: Long = 0
                                while (isActive && isRecording) {
                                    val readBytes = recorder.read(buffer, 0, buffer.size)
                                    if (readBytes > 0) {
                                        outputStream.write(buffer, 0, readBytes)
                                        totalAudioBytes += readBytes
                                    }
                                }
                                // Una vez que se detenga, escribimos el header correcto.
                                writeWavHeader(file, totalAudioBytes, sampleRate, channels = 1, bitsPerSample = 16)
                            }
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Iniciar grabación de audio"
                )
            }
        } else {
            // Interfaz mientras se está grabando
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Grabando: ${recordingTime}s",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    // Botón para cancelar la grabación
                    IconButton(
                        onClick = {
                            isRecording = false
                            recordingJob?.cancel() // Cancela la coroutine
                            audioRecorder?.stop()
                            audioRecorder?.release()
                            outputFile?.delete()
                            onRecordingCanceled()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Cancelar grabación"
                        )
                    }
                    // Botón para finalizar y enviar la grabación
                    IconButton(
                        onClick = {
                            isRecording = false
                            recordingJob?.cancel() // Termina la escritura del archivo
                            audioRecorder?.stop()
                            audioRecorder?.release()
                            // Reescribe el header final (por si la coroutine no lo completó)
                            outputFile?.let { file ->
                                // Calculamos el total de bytes del audio (excluyendo el header de 44 bytes)
                                val totalAudioBytes = file.length() - 44
                                writeWavHeader(file, totalAudioBytes, sampleRate = 16000, channels = 1, bitsPerSample = 16)
                                onAudioRecorded(file)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = "Enviar grabación"
                        )
                    }
                }
            }
            // Actualización del contador de tiempo
            LaunchedEffect(isRecording) {
                while (isRecording) {
                    delay(1000L)
                    recordingTime++
                }
            }
        }
    }
}

/**
 * Escribe el header WAV al inicio del archivo.
 * @param wavFile El archivo WAV en el que se escribe el header.
 * @param totalAudioLen La cantidad total de bytes de audio (sin incluir el header).
 * @param sampleRate La tasa de muestreo, por ejemplo, 16000.
 * @param channels Número de canales (1 para mono).
 * @param bitsPerSample Bits por muestra (16 para PCM 16 bits).
 */
fun writeWavHeader(wavFile: File, totalAudioLen: Long, sampleRate: Int, channels: Int, bitsPerSample: Int) {
    val byteRate = sampleRate * channels * bitsPerSample / 8
    val totalDataLen = totalAudioLen + 36
    val header = ByteArray(44)

    header[0] = 'R'.code.toByte()
    header[1] = 'I'.code.toByte()
    header[2] = 'F'.code.toByte()
    header[3] = 'F'.code.toByte()
    header[4] = (totalDataLen and 0xff).toByte()
    header[5] = ((totalDataLen shr 8) and 0xff).toByte()
    header[6] = ((totalDataLen shr 16) and 0xff).toByte()
    header[7] = ((totalDataLen shr 24) and 0xff).toByte()
    header[8] = 'W'.code.toByte()
    header[9] = 'A'.code.toByte()
    header[10] = 'V'.code.toByte()
    header[11] = 'E'.code.toByte()
    header[12] = 'f'.code.toByte()  // "fmt " chunk
    header[13] = 'm'.code.toByte()
    header[14] = 't'.code.toByte()
    header[15] = ' '.code.toByte()
    header[16] = 16  // Sub-chunk size para PCM
    header[17] = 0
    header[18] = 0
    header[19] = 0
    header[20] = 1  // Audio format, 1 = PCM
    header[21] = 0
    header[22] = channels.toByte()
    header[23] = 0
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = ((sampleRate shr 8) and 0xff).toByte()
    header[26] = ((sampleRate shr 16) and 0xff).toByte()
    header[27] = ((sampleRate shr 24) and 0xff).toByte()
    header[28] = (byteRate and 0xff).toByte()
    header[29] = ((byteRate shr 8) and 0xff).toByte()
    header[30] = ((byteRate shr 16) and 0xff).toByte()
    header[31] = ((byteRate shr 24) and 0xff).toByte()
    header[32] = (channels * bitsPerSample / 8).toByte()  // block align
    header[33] = 0
    header[34] = bitsPerSample.toByte()
    header[35] = 0
    header[36] = 'd'.code.toByte()
    header[37] = 'a'.code.toByte()
    header[38] = 't'.code.toByte()
    header[39] = 'a'.code.toByte()
    header[40] = (totalAudioLen and 0xff).toByte()
    header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
    header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
    header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

    RandomAccessFile(wavFile, "rw").use { raf ->
        raf.seek(0)
        raf.write(header, 0, 44)
    }
}