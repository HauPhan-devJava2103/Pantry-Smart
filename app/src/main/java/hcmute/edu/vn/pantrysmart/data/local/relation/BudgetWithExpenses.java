package hcmute.edu.vn.pantrysmart.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.Budget;
import hcmute.edu.vn.pantrysmart.data.local.entity.Expense;

// Budget kèm danh sách expenses trong tháng.
public class BudgetWithExpenses {

    @Embedded
    public Budget budget;

    @Relation(parentColumn = "id", entityColumn = "budget_id")
    public List<Expense> expenses;
}
