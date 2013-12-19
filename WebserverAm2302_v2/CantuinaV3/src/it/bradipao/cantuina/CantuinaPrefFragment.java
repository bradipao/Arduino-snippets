package it.bradipao.cantuina;

import android.os.Bundle;
import android.preference.PreferenceFragment;

public class CantuinaPrefFragment extends PreferenceFragment {
   @Override
   public void onCreate(Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);
       // load the preferences from an XML resource
       addPreferencesFromResource(R.xml.preferences);
   }
}
