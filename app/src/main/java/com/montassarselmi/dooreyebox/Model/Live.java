package com.montassarselmi.dooreyebox.Model;

import com.montassarselmi.dooreyebox.R;

public class Live extends EventHistory {


    public Live(int id, String time, String responder)
    {
        super(id,time, R.drawable.ic_live, "Door Check", responder , null);
    }



}
