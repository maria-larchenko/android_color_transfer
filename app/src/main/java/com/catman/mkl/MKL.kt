package com.catman.mkl

import android.graphics.Bitmap
import android.util.Log
import org.ejml.data.DMatrixRMaj
import org.ejml.simple.SimpleMatrix
import kotlin.math.max
import kotlin.math.sqrt


/**
 * Code is converted from this python script
 * https://github.com/mahmoudnafifi/colour_transfer_MKL/
 * **/
fun MKL(aMatrix: DMatrixRMaj,
        bMatrix: DMatrixRMaj,
        aMean: DMatrixRMaj,
        bMean: DMatrixRMaj,
):
        FloatArray
{
    /* ---- original MKL function ---- */
    val EPS = 2.2204e-16
    val A = SimpleMatrix.wrap(aMatrix)
    val B = SimpleMatrix.wrap(bMatrix)

    val eigA = A.eig()
    val Da2 = eigA.getEigenvalues()  // diagonal array^2 (actually vector)
    val Da = SimpleMatrix.filled(3, 3, sqrt(EPS))  // diagonal array
    Da.set(0, 0, sqrt(max(Da2[0].getReal(), 0.0) + EPS))
    Da.set(1, 1, sqrt(max(Da2[1].getReal(), 0.0) + EPS))
    Da.set(2, 2, sqrt(max(Da2[2].getReal(), 0.0) + EPS))

    val Ua_0 = eigA.getEigenVector(0)
    val Ua_1 = eigA.getEigenVector(1)
    val Ua_2 = eigA.getEigenVector(2)
    var Ua = Ua_0!!.combine(0, 1, Ua_1)!!.combine(0, 2, Ua_2) // !!. is a non-null asserted call

    val C = Da.mult(Ua.transpose()).mult(B).mult(Ua).mult(Da)
    val eigC = C.eig()

    val Dc2 = eigC.getEigenvalues()
    val Dc = SimpleMatrix.filled(3, 3, sqrt(EPS))
    Dc.set(0, 0, sqrt(max(Dc2[0].getReal(), 0.0) + EPS))
    Dc.set(1, 1, sqrt(max(Dc2[1].getReal(), 0.0) + EPS))
    Dc.set(2, 2, sqrt(max(Dc2[2].getReal(), 0.0) + EPS))

    val Uc_0 = eigC.getEigenVector(0)
    val Uc_1 = eigC.getEigenVector(1)
    val Uc_2 = eigC.getEigenVector(2)
    val Uc = Uc_0!!.combine(0, 1, Uc_1)!!.combine(0, 2, Uc_2)

    val DaInv = SimpleMatrix.diag(1.0 / Da[0, 0], 1.0 / Da[1, 1], 1.0 / Da[2, 2])
    val T = Ua.mult(DaInv).mult(Uc).mult(Dc).mult(Uc.transpose()).mult(DaInv).mult(Ua.transpose())
    Log.d("onSelectBitmap, DaInv", DaInv.toString())
    Log.d("onSelectBitmap, T", T.toString())

    /* ---- construction of final matrix from T ---- */
    /*  RESULT = (X0 - mX0) @ T + mX1 = X0 @ T + (mX1 - mX0 @ T)  */
    var mX0 = SimpleMatrix.wrap(aMean)
    var mX1 = SimpleMatrix.wrap(bMean)
    var TR = T.mult(mX0).scale(-1.0)
    var shift = mX1.plus(TR)
    /*
        R’ = a*R + b*G + c*B + d*A + e;
        G’ = f*R + g*G + h*B + i*A + j;
        B’ = k*R + l*G + m*B + n*A + o;
        A’ = p*R + q*G + r*B + s*A + t;
    */
    val F = floatArrayOf(
        T[0,0].toFloat(), T[0,1].toFloat(), T[0,2].toFloat(), 0f, shift[0].toFloat() * 255f,
        T[1,0].toFloat(), T[1,1].toFloat(), T[1,2].toFloat(), 0f, shift[1].toFloat() * 255f,
        T[2,0].toFloat(), T[2,1].toFloat(), T[2,2].toFloat(), 0f, shift[2].toFloat() * 255f,
        0f,               0f,               0f,               1f, 0f
    )
    return F
}


fun getMeanCov(bitmap: Bitmap): Array<DMatrixRMaj> {
    val width = bitmap.width
    val height = bitmap.height
    var rSum = 0.0
    var gSum = 0.0
    var bSum = 0.0
    var r2Sum = 0.0
    var g2Sum = 0.0
    var b2Sum = 0.0
    var rgSum = 0.0
    var rbSum = 0.0
    var gbSum = 0.0
    val totalPixels = (width * height).toDouble()

    for (y in 0 until height) {
        for (x in 0 until width) {
            val color = bitmap.getColor(x, y)
            val r = color.red().toDouble()
            val g = color.green().toDouble()
            val b = color.blue().toDouble()
            rSum += r
            gSum += g
            bSum += b
            r2Sum += r * r
            g2Sum += g * g
            b2Sum += b * b
            rgSum += r * g
            rbSum += r * b
            gbSum += g * b
        }
    }
    val rMean = rSum / totalPixels
    val gMean = gSum / totalPixels
    val bMean = bSum / totalPixels

    val mean = DMatrixRMaj(doubleArrayOf(rMean, gMean, bMean))
    val cov = DMatrixRMaj(
        arrayOf(
            doubleArrayOf(
                (r2Sum / totalPixels - rMean * rMean),
                (rgSum / totalPixels - rMean * gMean),
                (rbSum / totalPixels - rMean * bMean)
            ),
            doubleArrayOf(
                (rgSum / totalPixels - rMean * gMean),
                (g2Sum / totalPixels - gMean * gMean),
                (gbSum / totalPixels - gMean * bMean)
            ),
            doubleArrayOf(
                (rbSum / totalPixels - rMean * bMean),
                (gbSum / totalPixels - gMean * bMean),
                (b2Sum / totalPixels - bMean * bMean)
            )
        )
    )
    return arrayOf(mean, cov)
}