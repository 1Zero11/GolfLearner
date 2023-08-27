package com.vpered.golflearnner

import android.content.res.Resources
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.loader.content.CursorLoader
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.vpered.golflearnner.ui.theme.GolfLearnerTheme
import org.jetbrains.kotlinx.dl.api.core.shape.TensorShape
import org.jetbrains.kotlinx.dl.api.preprocessing.Operation
import org.jetbrains.kotlinx.dl.api.preprocessing.pipeline
import org.jetbrains.kotlinx.dl.impl.preprocessing.TensorLayout
import org.jetbrains.kotlinx.dl.impl.preprocessing.resize
import org.jetbrains.kotlinx.dl.impl.preprocessing.toFloatArray
import org.jetbrains.kotlinx.dl.onnx.inference.OnnxInferenceModel
import org.jetbrains.kotlinx.dl.onnx.inference.executionproviders.ExecutionProvider
import org.jetbrains.kotlinx.dl.onnx.inference.inferAndCloseUsing
import java.io.FileInputStream


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GolfLearnerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column() {
                        //Greeting("Android")
                        FileChooser(resources)
                    }

                }
            }
        }
    }
}

@Composable
fun FileChooser(resources: Resources){
    var showFilePicker by remember { mutableStateOf(false) }
    var pickedFileName by remember { mutableStateOf("") }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    val fileType = "mp4"
    FilePicker(showFilePicker, fileExtensions = listOf(fileType)) { path ->
        showFilePicker = false
        // do something with path

        if(path?.path != null) {
            pickedFileName = path.path
            val uri = Uri.parse(pickedFileName)

            val absPath = uri.path?.split(":")!![1]
            pickedFileName = absPath

            bitmap = GetFrame(absPath!!, 1000)

            Log.d("MYTAG", "Well?")

            //val modelBytes = resources.openRawResource(com.vpered.golflearnner.R.raw.kps_detector).readBytes()
            val modelProcessing = ModelProcessing()
            val kpsPreprocessing = modelProcessing.GetKPSPreprocessing()
            val model = modelProcessing.GetKPSModel(resources)

            val floatArr = Predict(model, bitmap!!, kpsPreprocessing)

            bitmap = modelProcessing.PaintBitmapKPS(
                modelProcessing.ConvertKPSPoints(
                    floatArr,
                    bitmap!!.height,
                    bitmap!!.width),
                bitmap!!
            )
        }

    }

    Button(
        onClick = {
            showFilePicker = true
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .height(48.dp)
            .clickable { showFilePicker = true },
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(text = "Choose File", style = TextStyle(color = Color.White))
    }

    if(pickedFileName != ""){
        Text(text = "File Chosen: " + pickedFileName)
    }

    if(bitmap != null){
    Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = "some useful description",
    )
    }

}

fun Predict(
    modelBytes: ByteArray,
    inputBitmap: Bitmap,
    preprocessing: Operation<Bitmap, Pair<FloatArray, TensorShape>>
): FloatArray {

    val model = OnnxInferenceModel(modelBytes)

    val o = Options()
    o.inScaled = false

    //bitmap = BitmapFactory.decodeResource(resources, com.vpered.golflearnner.R.raw.golf3, o).copy(Bitmap.Config.ARGB_8888, true)
    var bitmap = inputBitmap.copy(Bitmap.Config.ARGB_8888, true)

    val detections = model.inferAndCloseUsing(ExecutionProvider.CPU()) {
        var (inputData, shape) = preprocessing.apply(bitmap)

        val res = it.predictSoftly(inputData)
        return res
    }
}

fun GetFrame(path: String, frameTime: Long): Bitmap? {
    //var inputStream = FileInputStream(path);

    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    val bitmap = retriever.getFrameAtTime(frameTime,
        MediaMetadataRetriever.OPTION_CLOSEST)

    return bitmap
}