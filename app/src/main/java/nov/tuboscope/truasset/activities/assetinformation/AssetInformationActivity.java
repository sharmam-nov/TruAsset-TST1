package nov.tuboscope.truasset.activities.assetinformation;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.List;

import nov.tuboscope.truasset.R;
import nov.tuboscope.truasset.activities.GenericRFIDAvailableActivity;
import nov.tuboscope.truasset.adapters.FragmentViewPagerAdapter;
import nov.tuboscope.truasset.app.TruApp;
import nov.tuboscope.truasset.db.repo.AssetRepo;
import nov.tuboscope.truasset.fragments.asset.information.AssetActivityLogsFragment;
import nov.tuboscope.truasset.fragments.asset.information.AssetInformationFragment;
import nov.tuboscope.truasset.fragments.asset.information.AssetInspectionDataFragment;
import nov.tuboscope.truasset.fragments.asset.information.AssetPackingListsFragment;
import nov.tuboscope.truasset.models.asset.information.IAssetInspectionItemModel;
import nov.tuboscope.truasset.models.enums.UserType;
import nov.tuboscope.truasset.models.enums.service.ItemType;
import nov.tuboscope.truasset.models.updateble.Asset;
import nov.tuboscope.truasset.network.livedataloaders.assets.AssetsDataLoader;
import nov.tuboscope.truasset.views.dialogs.TAProgressDialog;
import nov.tuboscope.truasset.views.dialogs.relocate.GlobalVariable;
import nov.tuboscope.truasset.views.dialogs.replacedialog.RFIDFetchedListener;
import nov.tuboscope.truasset.views.dialogs.replacedialog.ReplaceRFIDDialog;
import nov.tuboscope.truasset.views.footer.AssetInformationFooterView;

/**
 * Created by Polgun on 27/10/2017.
 */

public class AssetInformationActivity
        extends GenericRFIDAvailableActivity
        implements AdapterView.OnItemSelectedListener, View.OnClickListener, ViewPager.OnPageChangeListener {

    private static final String tag = AssetInformationActivity.class.getSimpleName();
    private Button btnReplaceRFID;
    private AssetInformationFragment assetInfoFragment;
    private AssetInspectionDataFragment assetInspectionDataFragment;
    private AssetActivityLogsFragment assetActivityLogsFragment;
    private AssetPackingListsFragment assetPackingListsFragment;
    private String RFID_AssetInformationActivity;

    RFIDFetchedListener rfidFetchedListener; // replaceDialog
    protected Button btnVariable2;
    ViewPager viewPager;
    TabLayout tabLayout;
    FragmentViewPagerAdapter fragmentAdapter;

    AssetRepo assetRepo = new AssetRepo(this);
    Asset asset = null;
    AssetsDataLoader assetDataLoader = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.asset_information_activity);

        initRFID(savedInstanceState);

        initViews();
        assetDataLoader = new AssetsDataLoader(this, new TAProgressDialog(this));
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private AssetInformationFooterView getFooter() {
        return (AssetInformationFooterView) footerView;
    }

    @Override
    protected void initViews() {
        super.initViews();

        btnReplaceRFID = getFooter().getReplaceRFIDButton();

        btnReplaceRFID.setOnClickListener(this);
        setupViewPager();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void initRFID(Bundle savedInstanceState) {
        String rfid;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            rfid = extras == null ? null : extras.getString(Asset.RFID_KEY);
        } else {
            rfid = (String) savedInstanceState.getSerializable(Asset.RFID_KEY);
        }

        if (rfid == null) {
            rfid = getLastRFID();
        }

        RFID_AssetInformationActivity = rfid;
        asset = assetRepo.getItemByRFID(RFID_AssetInformationActivity);
    }

    private void setupViewPager() {
        viewPager = findViewById(R.id.vpPager);
        tabLayout = findViewById(R.id.tab_layout);

        assetInfoFragment = new AssetInformationFragment();
        assetInfoFragment.setRFID(RFID_AssetInformationActivity);

        assetInspectionDataFragment = new AssetInspectionDataFragment(ItemType.fromInt(asset.getEquipmentTypeCode()));
        assetActivityLogsFragment = new AssetActivityLogsFragment();
        assetPackingListsFragment = new AssetPackingListsFragment();

        fragmentAdapter = new FragmentViewPagerAdapter(getSupportFragmentManager());

       /* fragmentAdapter.addFragment(assetInfoFragment, "Asset information");
        fragmentAdapter.addFragment(assetInspectionDataFragment, "Inspection data");
        fragmentAdapter.addFragment(assetActivityLogsFragment, "Movement Details");
        fragmentAdapter.addFragment(assetPackingListsFragment, "Packing Lists");*/
        //WTBIZAPPS-32784 - TA: Customer scan rule changes
        Asset asset = assetRepo.getItemByRFID(GlobalVariable.RFIDScan );

        btnVariable2 = findViewById(R.id.btn_variable_2);
        if (TruApp.getPreferences().getTDUserType() == UserType.CUSTOMERS.getValue()) {
            if (asset.getOwnerCustomerPKey() == TruApp.getPreferences().getSelectedCustomerPKey()) {
                fragmentAdapter.addFragment(assetInfoFragment, "Asset information");
                fragmentAdapter.addFragment(assetInspectionDataFragment, "Inspection data");
                fragmentAdapter.addFragment(assetActivityLogsFragment, "Movement Details");
                fragmentAdapter.addFragment(assetPackingListsFragment, "Packing Lists");
                //Replace button
                btnVariable2.setVisibility(View.VISIBLE);
            } else{
                fragmentAdapter.addFragment(assetInfoFragment, "Asset information");
                //Replace button
                btnVariable2.setVisibility(View.GONE);
            }
        } else {
            //WTBIZAPPS-38158
            if(asset.getEquipmentTypeCode() == 8) { //Equipment
                fragmentAdapter.addFragment(assetInfoFragment, "Asset information");
            }
            else {
                fragmentAdapter.addFragment(assetInfoFragment, "Asset information");
                fragmentAdapter.addFragment(assetInspectionDataFragment, "Inspection data");
                fragmentAdapter.addFragment(assetActivityLogsFragment, "Movement Details");
                fragmentAdapter.addFragment(assetPackingListsFragment, "Packing Lists");
            }
        }

        viewPager.setAdapter(fragmentAdapter);
        viewPager.addOnPageChangeListener(this);
        tabLayout.setupWithViewPager(viewPager);
    }

    @Override
    public void newRFIDFetched(String rfid) {
        if (isReplaceDialogVisible()) {
            if (rfidFetchedListener != null)
                rfidFetchedListener.rfidFetched(rfid);
        } else
            super.newRFIDFetched(rfid);
    }

    private boolean isReplaceDialogVisible() {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment instanceof DialogFragment) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void newRFIDAvailable() {
        getIntent().putExtra(Asset.RFID_KEY, getLastRFID());
        Refresh();
    }

    @Override
    public void onClick(View view) {
        // btn_replace
        if (view.getId() == R.id.btn_variable_2) {
            showReplaceDialog();
        }
    }

    private void showReplaceDialog() {
        ReplaceRFIDDialog replaceRfidDialog = new ReplaceRFIDDialog();

        this.rfidFetchedListener = replaceRfidDialog;

        AssetRepo rfidRepo = new AssetRepo(this);

        if (asset != null) {
            replaceRfidDialog.setOldRFID(asset.getRFID(), asset.getSerialNo2());
            replaceRfidDialog.show(getSupportFragmentManager(), null);

        } else {
            throw new RuntimeException(getString(R.string.msg_rfid_not_found));

        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    private void showNetworkRequiredDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialogBuilder.setMessage(R.string.message_internet_required2);
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();
    }

    @Override
    public void onPageSelected(int position) {
        if (position > 0 && !TruApp.isInternetConnectionAvailable())
        {
            showNetworkRequiredDialog();
            return;
        }

        if (position == 1)
        {
            if (!assetInspectionDataFragment.hasData()) {

                assetInspectionDataFragment.setAssetInfo(assetInfoFragment.getAssetInfo());
                assetDataLoader.loadAssetInspectionData(asset, this.assetInspectionDataFragment);
            }
        }
        else
        if (position == 2)
        {
            if (!assetActivityLogsFragment.hasData()) {
                assetDataLoader.loadAssetActivityLogs(RFID_AssetInformationActivity, this.assetActivityLogsFragment);
            }
        }
        else
        if (position == 3)
        {
            if (!assetPackingListsFragment.hasData()) {
                assetDataLoader.loadPackingListsForAssetReceived(RFID_AssetInformationActivity, this.assetPackingListsFragment);
            }
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onProgressChanged(int progress) {

    }

    public interface AssetInspectionDataListener {
        void onAssetInspectionDataClick(IAssetInspectionItemModel item);
}
}
