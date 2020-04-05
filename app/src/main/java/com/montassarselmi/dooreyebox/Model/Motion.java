package com.montassarselmi.dooreyebox.Model;

import com.montassarselmi.dooreyebox.R;

public class Motion extends EventHistory {



    public Motion(int id, String time, String visitorImage)
    {
        super(id,time, R.drawable.ic_motion, "Motion", null , visitorImage);
    }
    public Motion(int id, String time)
    {
        super(id,time, R.drawable.ic_motion, "Motion", null , null);
    }


}
