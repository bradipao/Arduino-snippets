package it.bradipao.podval;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ArduinoAdapter extends BaseAdapter {

   private static ArrayList<ArduinoItem> arduList;
   private ArduinoItem tmp = null;
   private LayoutInflater mInflater;
   
   // mandatory abstract methods
   @Override
   public int getCount() { return arduList.size(); }
   @Override
   public Object getItem(int position) { return arduList.get(position); }
   @Override
   public long getItemId(int position) { return position; }

   // constructor
   public ArduinoAdapter(Context ctx,ArrayList<ArduinoItem> res) {
      arduList = res;
      mInflater = LayoutInflater.from(ctx);
   }
   
   @Override
   public View getView(int position,View convertView,ViewGroup parent) {
      // view holder of findViewById references
      ViewHolder holder;
      
   // check view for reuse
      if (convertView == null) {
         // inflate view
         convertView = mInflater.inflate(R.layout.lv_arduitem,null);
         // create and populate viewholder
         holder = new ViewHolder();
         holder.tvName = (TextView) convertView.findViewById(R.id.tvName);
         holder.tvOk = (TextView) convertView.findViewById(R.id.tvOk);
         holder.tvHwid = (TextView) convertView.findViewById(R.id.tvHwid);
         holder.tvIp = (TextView) convertView.findViewById(R.id.tvIp);
         // store viewholder in view (because it will be recycled anyway)
         convertView.setTag(holder);
      } else {
         // retrieve stored viewholder (replace expensive findViewById)
         holder = (ViewHolder) convertView.getTag();
      }
      
      // get item
      tmp = arduList.get(position);
      // set name and icon in views
      holder.tvName.setText(tmp.sName);
      holder.tvHwid.setText(tmp.sHwid);
      holder.tvIp.setText(tmp.sIp);
      if (tmp.bOk) {
         holder.tvOk.setText("ONLINE");
         holder.tvOk.setTextColor(0xff009900);
      }
      else {
         holder.tvOk.setText("OFFLINE");
         holder.tvOk.setTextColor(0xff990000);
      }
      
      // return
      return convertView;
      
   }

   // static view holder
   static class ViewHolder {
      TextView tvName,tvOk,tvHwid,tvIp;
   }
   
}
