package com.montassarselmi.dooreyebox.Model;


import android.content.res.Resources;

import com.montassarselmi.dooreyebox.R;

public class Ring extends EventHistory {


    public Ring(int id, String time, String responder, String visitorImage)
    {
        super(id,time, "Ring",responder, visitorImage);

    }
    public Ring(int id, String time)
    {
        super(id,time,  "Ring", "No one", null);
    }


}
