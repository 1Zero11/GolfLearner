package com.vpered.golflearnner

import android.R.attr
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.os.Bundle
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
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import com.vpered.golflearnner.ui.theme.GolfLearnerTheme
import org.jetbrains.kotlinx.dl.api.preprocessing.pipeline
import org.jetbrains.kotlinx.dl.impl.preprocessing.TensorLayout
import org.jetbrains.kotlinx.dl.impl.preprocessing.resize
import org.jetbrains.kotlinx.dl.impl.preprocessing.toFloatArray
import org.jetbrains.kotlinx.dl.onnx.inference.OnnxInferenceModel
import org.jetbrains.kotlinx.dl.onnx.inference.executionproviders.ExecutionProvider
import org.jetbrains.kotlinx.dl.onnx.inference.inferAndCloseUsing


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
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "$name!",
        modifier = modifier
    )
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

        if(path?.path != null)
            pickedFileName = path.path;

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

    val modelBytes = resources.openRawResource(com.vpered.golflearnner.R.raw.kps_detector).readBytes()
    bitmap = Predict(modelBytes, resources)

    if(bitmap != null){
    Image(
        bitmap = bitmap!!.asImageBitmap(),
        contentDescription = "some useful description",
    )
    }

}

fun Predict(modelBytes: ByteArray, resources: Resources): Bitmap {

    val model = OnnxInferenceModel(modelBytes)

    val preprocessing = pipeline<Bitmap>()
        .resize {
            outputHeight = 192
            outputWidth = 192
        }
        .toFloatArray { layout = TensorLayout.NHWC }

    val o = Options()
    o.inScaled = false

    var bitmap = BitmapFactory.decodeResource(resources, com.vpered.golflearnner.R.raw.golf3, o).copy(Bitmap.Config.ARGB_8888, true)
    val bitmap2 = BitmapFactory.decodeResource(resources, com.vpered.golflearnner.R.raw.golf_guy)

    val detections = model.inferAndCloseUsing(ExecutionProvider.CPU()) {
        var (inputData, shape) = preprocessing.apply(bitmap)

        val (inputData2, _) = preprocessing.apply(bitmap2)

        //inputData += inputData2
        val test = arrayOf(inputData)
        Log.d("MYTAG", "firstMessage")
        val res = it.predictSoftly(inputData)

        var res2 = it.predictRaw(inputData)



        val resArr = Array(17) { i ->
            FloatArray(3) { j ->
                if(j==0) {
                    res[i * 3 + j] * bitmap.height
                } else if(j==1){
                    res[i * 3 + j]*bitmap.width
                } else{
                    res[i * 3 + j]
                }

            }
        }

        val size: Int = Math.min(20, 20)


        var localCanvas = Canvas(bitmap)
        localCanvas.setDrawFilter(
            PaintFlagsDrawFilter(
                0,
                Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG
            )
        )
        val paint = Paint()
        paint.color = Color.Red.toArgb()
        paint.setAntiAlias(true)

        for (i in resArr.indices) {
            val rectF = RectF(resArr[i][1], resArr[i][0], resArr[i][1]+20, resArr[i][0]+20)
            localCanvas.drawOval(rectF, paint)
        }
        paint.setXfermode(PorterDuffXfermode(PorterDuff.Mode.SRC_IN))
        localCanvas.drawBitmap(bitmap, 0f, 0f, paint)
        paint.setXfermode(null)

        Log.d("MYTAG", "Okay")

        return bitmap
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GolfLearnerTheme {
        Greeting("Android")
    }
}