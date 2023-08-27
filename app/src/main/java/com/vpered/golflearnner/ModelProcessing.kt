package com.vpered.golflearnner

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PaintFlagsDrawFilter
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.kotlinx.dl.api.core.shape.TensorShape
import org.jetbrains.kotlinx.dl.api.preprocessing.Operation
import org.jetbrains.kotlinx.dl.api.preprocessing.pipeline
import org.jetbrains.kotlinx.dl.impl.preprocessing.TensorLayout
import org.jetbrains.kotlinx.dl.impl.preprocessing.resize
import org.jetbrains.kotlinx.dl.impl.preprocessing.toFloatArray

class ModelProcessing {
    public fun GetYolovModel(resources: Resources): ByteArray{
        return resources.openRawResource(com.vpered.golflearnner.R.raw.yolov8s_trained).readBytes()
    }

    public fun GetKPSModel(resources: Resources): ByteArray{
        return resources.openRawResource(com.vpered.golflearnner.R.raw.kps_detector).readBytes()
    }

    public fun GetSwingModel(resources: Resources): ByteArray{
        return resources.openRawResource(com.vpered.golflearnner.R.raw.swing).readBytes()
    }

    public fun GetKPSPreprocessing(): Operation<Bitmap, Pair<FloatArray, TensorShape>> {
        return pipeline<Bitmap>()
            .resize {
                outputHeight = 192
                outputWidth = 192
            }
            .toFloatArray { layout = TensorLayout.NHWC }
    }

    public fun GetYolovPreprocessing(): Operation<Bitmap, Pair<FloatArray, TensorShape>> {
        return pipeline<Bitmap>()
            .resize {
                outputHeight = 640
                outputWidth = 640
            }
            .toFloatArray { layout = TensorLayout.NHWC }
    }

    public fun GetSwingPreprocessing(): Operation<Bitmap, Pair<FloatArray, TensorShape>> {
        return pipeline<Bitmap>()
            .resize {
                outputHeight = 160
                outputWidth = 160
            }
            .toFloatArray { layout = TensorLayout.NHWC }
    }

    public fun ConvertKPSPoints(points: FloatArray, height: Int, width: Int): Array<FloatArray>{
        val resArr = Array(17) { i ->
            FloatArray(3) { j ->
                if(j==0) {
                    points[i * 3 + j] * height
                } else if(j==1){
                    points[i * 3 + j]* width
                } else{
                    points[i * 3 + j]
                }

            }
        }
        return resArr
    }

    public fun PaintBitmapKPS(points: Array<FloatArray>, bitmap: Bitmap): Bitmap{
        val resArr = points

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