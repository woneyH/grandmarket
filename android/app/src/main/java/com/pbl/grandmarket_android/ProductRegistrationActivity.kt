package com.pbl.grandmarket_android

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ProductRegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_registration)

        val etProductName = findViewById<EditText>(R.id.etProductName)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)

        // 스캐너에서 넘어온 품목명 받기
        val detectedClass = intent.getStringExtra("DETECTED_CLASS")
        if (detectedClass != null) {
            etProductName.setText(detectedClass)
            Toast.makeText(this, "AI가 품목을 자동 입력했습니다.", Toast.LENGTH_SHORT).show()
        }

        btnSubmit.setOnClickListener {
            val name = etProductName.text.toString()
            val price = etPrice.text.toString()

            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: 실제 서버나 DB에 상품 등록 API 호출
            Toast.makeText(this, "${name}이(가) ${price}원에 등록되었습니다!", Toast.LENGTH_LONG).show()
            finish()
        }
    }
}
