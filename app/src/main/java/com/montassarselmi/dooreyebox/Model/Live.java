package com.montassarselmi.dooreyebox.Model;

import com.montassarselmi.dooreyebox.R;

import java.util.Date;

public class Live extends EventHistory {


    public Live(int id, Date time, String responder)
    {
        super(id,time, "Door Check", responder , null);
    }



}
