package com.montassarselmi.dooreyebox.Model;


import com.montassarselmi.dooreyebox.R;

public class Ring extends EventHistory {


    public Ring(int id, String time, String responder, String visitorImage)
    {
        super(id,time, R.drawable.ic_ring, "Ring",responder, visitorImage);

    }
    public Ring(int id, String time, String responder)
    {
        super(id,time, R.drawable.ic_ring, "Ring",responder, null);
    }


}
