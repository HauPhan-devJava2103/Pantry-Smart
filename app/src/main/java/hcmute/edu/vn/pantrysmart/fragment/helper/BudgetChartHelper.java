package hcmute.edu.vn.pantrysmart.fragment.helper;

import android.graphics.Color;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.pantrysmart.data.local.dao.ExpenseDao;

public class BudgetChartHelper {

    private final BarChart barChart;
    private final BarChart barChartMonthly;

    public BudgetChartHelper(BarChart barChart, BarChart barChartMonthly) {
        this.barChart = barChart;
        this.barChartMonthly = barChartMonthly;
    }

    
    // BarChart — Chi tiêu 7 ngày
    public void updateBarChart(List<ExpenseDao.DailyStat> dailyStats, long weekStart) {
        // Tạo map ngày → tổng để tra cứu nhanh
        Map<String, Double> statMap = new HashMap<>();
        SimpleDateFormat dayFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (dailyStats != null) {
            for (ExpenseDao.DailyStat stat : dailyStats) {
                statMap.put(dayFmt.format(stat.expense_date), stat.total);
            }
        }

        // Tạo 7 entries cho 7 ngày (Thứ 2 → CN)
        final String[] dayLabels = { "T2", "T3", "T4", "T5", "T6", "T7", "CN" };
        List<BarEntry> entries = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStart);

        for (int i = 0; i < 7; i++) {
            String key = dayFmt.format(cal.getTime());
            double val = statMap.containsKey(key) ? statMap.get(key) : 0;
            entries.add(new BarEntry(i, (float) val));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(Color.parseColor("#00BC7D"));
        dataSet.setDrawValues(false); // Tắt nhãn giá trị trên cột

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChart.setData(barData);

        // Cấu hình trục X
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < 7) ? dayLabels[idx] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#99A1AF"));
        xAxis.setTextSize(10f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(6.5f);

        // Ẩn các thành phần thừa
        configureChartCommon(barChart);
        barChart.animateY(400);
        barChart.invalidate();
    }

    
    // BarChart — Chi tiêu tháng (4~5 tuần)
    
    public void updateBarChartMonthly(List<ExpenseDao.WeeklyStat> weeklyStats, int weeksInMonth) {
        weeksInMonth = Math.max(4, Math.min(weeksInMonth, 5));

        Map<Integer, Double> statMap = new HashMap<>();
        if (weeklyStats != null) {
            for (ExpenseDao.WeeklyStat stat : weeklyStats) {
                statMap.put(stat.week_index, stat.total);
            }
        }

        final int finalWeeks = weeksInMonth;
        final String[] weekLabels = new String[finalWeeks];
        List<BarEntry> entries = new ArrayList<>();

        // Tìm tuần hiện tại
        Calendar now = Calendar.getInstance();
        Calendar monthStartCal = Calendar.getInstance();
        monthStartCal.set(Calendar.DAY_OF_MONTH, 1);
        monthStartCal.set(Calendar.HOUR_OF_DAY, 0);
        monthStartCal.set(Calendar.MINUTE, 0);
        monthStartCal.set(Calendar.SECOND, 0);
        monthStartCal.set(Calendar.MILLISECOND, 0);
        int currentWeekIdx = (int) ((now.getTimeInMillis() - monthStartCal.getTimeInMillis()) / 604800000);
        currentWeekIdx = Math.max(0, Math.min(currentWeekIdx, finalWeeks - 1));

        for (int i = 0; i < finalWeeks; i++) {
            weekLabels[i] = "Tuần " + (i + 1);
            double val = statMap.containsKey(i) ? statMap.get(i) : 0;
            entries.add(new BarEntry(i, (float) val));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setDrawValues(false);

        // Highlight tuần hiện tại (nếu là tháng hiện tại)
        int[] colors = new int[finalWeeks];
        for (int i = 0; i < finalWeeks; i++) {
            if (currentWeekIdx == -1) {
                // Không phải tháng hiện tại -> tất cả màu chính
                colors[i] = Color.parseColor("#00BC7D");
            } else {
                // Có highlight tuần này
                colors[i] = (i == currentWeekIdx)
                        ? Color.parseColor("#00BC7D")
                        : Color.parseColor("#86EFAC"); // Màu xanh nhạt nhưng rõ hơn (#A7F3D0 cũ quá mờ)
            }
        }
        dataSet.setColors(colors);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.7f); // Tăng độ rộng cột cho "đầy đặn" hơn
        barChartMonthly.setData(barData);

        // Cấu hình trục X
        XAxis xAxis = barChartMonthly.getXAxis();
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < finalWeeks) ? weekLabels[idx] : "";
            }
        });
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.parseColor("#99A1AF"));
        xAxis.setTextSize(10f);
        xAxis.setAxisMinimum(-0.5f);
        xAxis.setAxisMaximum(finalWeeks - 0.5f);

        configureChartCommon(barChartMonthly);
        barChartMonthly.animateY(600);
        barChartMonthly.invalidate();
    }

    
    // Cấu hình chung cho biểu đồ
    
    private void configureChartCommon(BarChart chart) {
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisLeft().setTextColor(Color.parseColor("#99A1AF"));
        chart.getAxisLeft().setTextSize(9f);
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value == 0)
                    return "0";
                if (value >= 1_000_000)
                    return (int) (value / 1_000_000) + "tr";
                if (value >= 1_000)
                    return (int) (value / 1_000) + "k";
                return String.valueOf((int) value);
            }
        });
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(4f);
    }
}
