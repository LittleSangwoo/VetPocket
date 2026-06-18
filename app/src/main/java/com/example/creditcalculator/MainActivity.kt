package com.example.creditcalculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.pow

class MainActivity : AppCompatActivity() {

    private lateinit var etAmount: EditText
    private lateinit var etRate: EditText
    private lateinit var etTerm: EditText
    private lateinit var btnCalculate: Button
    private lateinit var tvError: TextView
    private lateinit var llResults: LinearLayout
    private lateinit var tvPayment: TextView
    private lateinit var tvTotalSum: TextView
    private lateinit var tvOverpayment: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()

        btnCalculate.setOnClickListener {
            calculateCredit()
        }
    }

    private fun initViews() {
        etAmount = findViewById(R.id.etAmount)
        etRate = findViewById(R.id.etRate)
        etTerm = findViewById(R.id.etTerm)
        btnCalculate = findViewById(R.id.btnCalculate)
        tvError = findViewById(R.id.tvError)
        llResults = findViewById(R.id.llResults)
        tvPayment = findViewById(R.id.tvPayment)
        tvTotalSum = findViewById(R.id.tvTotalSum)
        tvOverpayment = findViewById(R.id.tvOverpayment)
    }

    private fun calculateCredit() {
        tvError.visibility = View.GONE
        llResults.visibility = View.GONE

        // 1. Считывание данных
        val amountStr = etAmount.text.toString().trim()
        val rateStr = etRate.text.toString().trim()
        val termStr = etTerm.text.toString().trim()

        // 2. Валидация на пустые поля и корректность чисел
        val amount = amountStr.toLongOrNull()
        val rate = rateStr.toDoubleOrNull()
        val term = termStr.toIntOrNull()

        if (amount == null || rate == null || term == null) {
            showError("Пожалуйста, заполните все поля корректными числовыми значениями.")
            return
        }

        // 3. Валидация диапазонов
        if (amount !in 1000..10_000_000) {
            showError("Сумма кредита должна быть от 1 000 до 10 000 000 руб.")
            return
        }
        if (rate < 0.1 || rate > 30.0) {
            showError("Процентная ставка должна быть от 0.1 до 30%.")
            return
        }
        if (term !in 1..360) {
            showError("Срок кредита должен быть от 1 до 360 месяцев.")
            return
        }

        // 4. Математические расчеты
        val s = amount.toDouble()
        val r = rate
        val n = term.toDouble()

        // Формула 1. Месячная процентная ставка
        val i = r / 100.0 / 12.0

        // Формула 2. Аннуитетный платёж
        val mathPow = (1.0 + i).pow(n)
        val a = s * (i * mathPow) / (mathPow - 1.0)

        // Формула 3. Общая сумма выплат
        val sum = a * n

        // Формула 4. Переплата по кредиту
        val over = sum - s

        // 5. Вывод результатов с форматированием (2 знака после запятой)
        tvPayment.text = String.format(Locale.getDefault(), "Ежемесячный платёж: %.2f руб.", a)
        tvTotalSum.text = String.format(Locale.getDefault(), "Общая сумма выплат: %.2f руб.", sum)
        tvOverpayment.text = String.format(Locale.getDefault(), "Переплата: %.2f руб.", over)

        llResults.visibility = View.VISIBLE
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }
}