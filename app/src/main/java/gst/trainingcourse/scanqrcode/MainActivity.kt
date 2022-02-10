package gst.trainingcourse.scanqrcode

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    private lateinit var btnScanBarCode :Button
    private lateinit var ivQrCode :ImageView
    private lateinit var tvResult:TextView

    private val CAMERA_PERMISSION_CODE =123
    private val READ_STORAGE_PERMISSION_CODE = 113
    private val WRITE_STORAGE_PERMISSION_CODE = 113

    private lateinit var cameraLauncher:ActivityResultLauncher<Intent>
    private lateinit var galleryLauncher :ActivityResultLauncher<Intent>

    private lateinit var inputImage :InputImage
    private lateinit var barcodeScanner :BarcodeScanner

    private val TAG="MyTag"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScanBarCode=findViewById(R.id.btn_scan)
        ivQrCode =findViewById(R.id.img_qr_code)
        tvResult=findViewById(R.id.txt_result)

        barcodeScanner=BarcodeScanning.getClient()

        cameraLauncher=registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object :ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data =result?.data

                    try{
                        val photo = data?.extras?.get("data") as Bitmap
                        inputImage = InputImage.fromBitmap(photo,0)
                        processQr()

                    }catch (e:Exception){
                        Log.d(TAG ,"onActivityResult : "+e.message)
                    }
                }

            }
        )

        galleryLauncher=registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            object :ActivityResultCallback<ActivityResult>{
                override fun onActivityResult(result: ActivityResult?) {
                    val data = result?.data

                    inputImage = data?.data?.let { InputImage.fromFilePath(this@MainActivity, it) }!!
                    processQr()
                }

            }
        )

        btnScanBarCode.setOnClickListener {
            val options = arrayOf("camera","gallery")
            val builder =AlertDialog.Builder(this)
            builder.setTitle("Pick a option")
            builder.setItems(options , DialogInterface.OnClickListener{dialog, which ->
                if(which == 0){
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                }else{
                    val storageIntent = Intent()
                    storageIntent.setType("image/*")
                    storageIntent.setAction(Intent.ACTION_GET_CONTENT)
                    galleryLauncher.launch(storageIntent)
                }
            })

            builder.show()
        }

    }

    private fun processQr(){
        ivQrCode.visibility= View.GONE
        tvResult.visibility=View.VISIBLE

        barcodeScanner.process(inputImage).addOnSuccessListener {
            for(barcode :Barcode in it){
                val valueType = barcode.valueType
                when (valueType) {
                    Barcode.TYPE_WIFI -> {
                        val ssid = barcode.wifi!!.ssid
                        val password = barcode.wifi!!.password
                        val type = barcode.wifi!!.encryptionType

                        tvResult.text="ssid ${ssid} \n password ${password} \n type ${type}"
                    }
                    Barcode.TYPE_URL -> {
                        val title = barcode.url!!.title
                        val url = barcode.url!!.url

                        tvResult.text="title ${title} \n url ${url}"
                    }
                    Barcode.TYPE_TEXT ->{
                        val data = barcode.displayValue
                        tvResult.text="Result ${data}"
                    }
                }
            }
        }.addOnFailureListener {
            Log.d(TAG, "processQr: ${it.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermission(android.Manifest.permission.CAMERA , CAMERA_PERMISSION_CODE)
    }


    private fun checkPermission(permission :String ,requestCode :Int){
        if(ContextCompat.checkSelfPermission(this,permission) == PackageManager.PERMISSION_DENIED){
            //Take Permission
            ActivityCompat.requestPermissions(this , arrayOf(permission),requestCode)
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode ==CAMERA_PERMISSION_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                checkPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE , READ_STORAGE_PERMISSION_CODE)
            }else{
                Toast.makeText(this,"Camera Permission Denied ",Toast.LENGTH_SHORT).show()
            }
        }else if(requestCode == READ_STORAGE_PERMISSION_CODE){
            if((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                checkPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE , WRITE_STORAGE_PERMISSION_CODE)
            }else{
                Toast.makeText(this,"Storage Permission Denied ",Toast.LENGTH_SHORT).show()
            }
        }
        else if(requestCode == WRITE_STORAGE_PERMISSION_CODE){
            if(!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)){
                Toast.makeText(this,"Storage Permission Denied ",Toast.LENGTH_SHORT).show()
            }
        }
    }
}