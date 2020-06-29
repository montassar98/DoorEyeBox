package com.montassarselmi.dooreyebox.Model;

import com.montassarselmi.dooreyebox.R;

import java.util.Date;

public class Motion extends EventHistory {


    public Motion(){
        super();
    }

    public Motion(int id, Date time, String visitorImage)
    {
        super(id,time, "Motion", null , visitorImage);
    }
    public Motion(int id, Date time)
    {
        super(id,time, "Motion", null , null);
    }



}
