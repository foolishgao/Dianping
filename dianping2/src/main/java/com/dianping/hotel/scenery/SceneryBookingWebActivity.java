package com.dianping.hotel.scenery;

import android.os.Bundle;
import com.dianping.base.web.ui.NovaZeusActivity;
import com.dianping.base.web.ui.NovaZeusFragment;
import com.dianping.hotel.commons.web.HotelZeusFragment;

public class SceneryBookingWebActivity extends NovaZeusActivity
{
  protected Class<? extends NovaZeusFragment> getEfteFragmentClass()
  {
    return HotelZeusFragment.class;
  }

  public void onCreate(Bundle paramBundle)
  {
    super.onCreate(paramBundle);
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     com.dianping.hotel.scenery.SceneryBookingWebActivity
 * JD-Core Version:    0.6.0
 */