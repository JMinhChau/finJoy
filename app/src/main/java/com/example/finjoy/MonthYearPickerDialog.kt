import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment
import com.example.finjoy.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

class MonthYearPickerDialog : DialogFragment() {
    private var listener: DatePickerDialog.OnDateSetListener? = null

    fun setListener(listener: DatePickerDialog.OnDateSetListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val calendar = Calendar.getInstance()
        val view = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)

        val monthPicker = view.findViewById<NumberPicker>(R.id.monthPicker).apply {
            this.minValue = 0
            this.maxValue = 11
            this.value = calendar.get(Calendar.MONTH)
            this.displayedValues = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        }

        val yearPicker = view.findViewById<NumberPicker>(R.id.yearPicker).apply {
            this.minValue = 2020
            this.maxValue = calendar.get(Calendar.YEAR)
            this.value = calendar.get(Calendar.YEAR)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Month and Year")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                listener?.onDateSet(null, yearPicker.value, monthPicker.value, 1)
            }
            .setNegativeButton("Cancel", null)
            .create()
    }
}