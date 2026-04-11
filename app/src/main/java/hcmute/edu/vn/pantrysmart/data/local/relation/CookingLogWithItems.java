package hcmute.edu.vn.pantrysmart.data.local.relation;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLog;
import hcmute.edu.vn.pantrysmart.data.local.entity.CookingLogItem;

// CookingLog kèm chi tiết nguyên liệu đã trừ.
public class CookingLogWithItems {

    @Embedded
    public CookingLog cookingLog;

    @Relation(parentColumn = "id", entityColumn = "cooking_log_id")
    public List<CookingLogItem> items;
}
