package com.vpered.golflearnner

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

        val modelBytes = resources.openRawResource(com.vpered.golflearnner.R.raw.kps_detector).readBytes()
        Predict(modelBytes, resources)

        setContent {
            GolfLearnerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column() {
                        //Greeting("Android")
                        FileChooser()
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
fun FileChooser(){
    var showFilePicker by remember { mutableStateOf(false) }
    var pickedFileName by remember { mutableStateOf("") }

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

}

fun Predict(modelBytes: ByteArray, resources: Resources) {

    val model = OnnxInferenceModel(modelBytes)

    val preprocessing = pipeline<Bitmap>()
        .resize {
            outputHeight = 192
            outputWidth = 192
        }
        .toFloatArray { layout = TensorLayout.NHWC }

    val o = Options()
    o.inScaled = false

    val bitmap = BitmapFactory.decodeResource(resources, com.vpered.golflearnner.R.raw.golf, o)
    val bitmap2 = BitmapFactory.decodeResource(resources, com.vpered.golflearnner.R.raw.golf_guy)

    val detections = model.inferAndCloseUsing(ExecutionProvider.CPU()) {
        var (inputData, shape) = preprocessing.apply(bitmap)

        val (inputData2, _) = preprocessing.apply(bitmap2)

        //inputData += inputData2
        val test = arrayOf(inputData)
        Log.d("MYTAG", "firstMessage")
        val res = it.predictSoftly(inputData)



        val resArr = Array(17) { i ->
            FloatArray(3) { j ->
                if(j==0) {
                    res[i * 3 + j] * bitmap.width
                } else if(j==1){
                    res[i * 3 + j]*bitmap.height
                } else{
                    res[i * 3 + j]
                }

            }
        }
        Log.d("MYTAG", "Okay")
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GolfLearnerTheme {
        Greeting("Android")
    }
}