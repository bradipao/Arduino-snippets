package it.bradipao.podval;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;

public class PrefActivity extends Activity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       // display the fragment as the main content.
       getFragmentManager().beginTransaction()
               .replace(android.R.id.content,new PrefFragment())
               .commit();
       // action bar UP button
       ActionBar actionBar = getActionBar();
       actionBar.setDisplayHomeAsUpEnabled(true);
   }
   
   // menu management
   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
      // respond to the action bar's UP/HOME button
      case android.R.id.home:
         finish();
         return true;
      }
      return super.onOptionsItemSelected(item);
   }
}
