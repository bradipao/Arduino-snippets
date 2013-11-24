package it.bradipao.cantuina;

import android.app.Activity;
import android.os.Bundle;

public class CantuinaPrefActivity extends Activity {
   @Override
   protected void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       // display the fragment as the main content.
       getFragmentManager().beginTransaction()
               .replace(android.R.id.content,new CantuinaPrefFragment())
               .commit();
   }
}
