package hcmute.edu.vn.pantrysmart.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// Ngân sách theo tháng
@Entity(tableName = "budgets")
public class Budget {

    @PrimaryKey(autoGenerate = true)
    private int id;

    // Ngân sách tối đa theo tháng
    @ColumnInfo(name = "monthly_limit")
    private double monthlyLimit;
    @ColumnInfo(name = "weekly_limit")
    private double weeklyLimit;

    // Tháng
    @ColumnInfo(name = "month")
    private int month;

    // Năm
    @ColumnInfo(name = "year")
    private int year;

    public Budget() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public double getWeeklyLimit() {
        return weeklyLimit;
    }

    public void setWeeklyLimit(double weeklyLimit) {
        this.weeklyLimit = weeklyLimit;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }
}
