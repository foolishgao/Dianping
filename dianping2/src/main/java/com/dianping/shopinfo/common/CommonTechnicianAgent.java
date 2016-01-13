package com.dianping.shopinfo.common;

import android.os.Bundle;
import com.dianping.archive.DPObject;
import com.dianping.dataservice.mapi.BasicMApiRequest;
import com.dianping.dataservice.mapi.CacheType;
import com.dianping.dataservice.mapi.MApiService;
import com.dianping.shopinfo.base.BaseTechnicianAgent;
import com.dianping.shopinfo.fragment.ShopInfoFragment;

public class CommonTechnicianAgent extends BaseTechnicianAgent
{
  public CommonTechnicianAgent(Object paramObject)
  {
    super(paramObject);
  }

  public DPObject getData(Bundle paramBundle)
  {
    return this.techniciansInfo;
  }

  public void onCreate(Bundle paramBundle)
  {
    sendRequest();
  }

  public void sendRequest()
  {
    StringBuilder localStringBuilder = new StringBuilder("http://mapi.dianping.com/technician/gettechnicians.bin?");
    localStringBuilder.append("shopid=").append(shopId());
    this.request = BasicMApiRequest.mapiGet(localStringBuilder.toString(), CacheType.NORMAL);
    getFragment().mapiService().exec(this.request, this);
  }
}

/* Location:           C:\Users\xuetong\Desktop\dazhongdianping7.9.6\ProjectSrc\classes-dex2jar.jar
 * Qualified Name:     com.dianping.shopinfo.common.CommonTechnicianAgent
 * JD-Core Version:    0.6.0
 */