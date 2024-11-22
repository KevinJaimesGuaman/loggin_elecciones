package com.example.loggin_elecciones

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class administrador_CrearEditar : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_administrador_crear_editar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val buttonVolver = findViewById<ImageButton>(R.id.volver)
        buttonVolver.setOnClickListener {
            val intent = Intent(this, home_administrador::class.java)
            startActivity(intent)
        }
        val buttonFechaInicio: Button = findViewById(R.id.button_fecha_inicio)
        val buttonTimePicker: Button = findViewById(R.id.button_timepicker_inicio)

        val calendar = Calendar.getInstance()

        // Configurar el botón de fecha
        buttonFechaInicio.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                buttonFechaInicio.text = formattedDate
            }, year, month, day)

            datePicker.show()
        }
        // Configurar el botón de hora
        buttonTimePicker.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePicker = TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                // Determinar AM o PM
                val amPm = if (selectedHour >= 12) "PM" else "AM"

                // Convertir la hora al formato de 12 horas
                val hour12 = if (selectedHour % 12 == 0) 12 else selectedHour % 12

                // Formatear la hora con AM/PM
                val formattedTime = String.format("%02d:%02d %s", hour12, selectedMinute, amPm)
                buttonTimePicker.text = formattedTime
            }, hour, minute, false) // Cambiar a true si quieres usar el formato de 24 horas.

            timePicker.show()
        }

    }


    class CalendarDialogFragment(private val onDateSelected: (String) -> Unit) : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val builder = android.app.AlertDialog.Builder(requireActivity())

            // Inflar el layout para el calendario
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.dialog_calendario, null)

            val calendarView = view.findViewById<CalendarView>(R.id.calendarView)
            val textViewFechaSeleccionada = view.findViewById<TextView>(R.id.textViewFechaSeleccionada)

            // Configurar el Listener para la selección de fecha
            calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
                val selectedDate = "$dayOfMonth/${month + 1}/$year"
                textViewFechaSeleccionada.text = "Fecha seleccionada: $selectedDate"
                onDateSelected(selectedDate)  // Retornar la fecha seleccionada
                dismiss()  // Cerrar el calendario después de seleccionar una fecha
            }

            builder.setView(view)
            return builder.create()
        }
    }
}
