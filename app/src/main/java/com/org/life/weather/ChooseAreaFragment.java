package com.org.life.weather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.org.life.weather.db.City;
import com.org.life.weather.db.County;
import com.org.life.weather.db.Province;
import com.org.life.weather.util.HttpUtil;
import com.org.life.weather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;

    private ProgressDialog mProgressDialog;

    private TextView mTitleText;

    private Button mBtnBack;

    private ListView mListView;

    private ArrayAdapter<String> mAdapter;

    private List<String> dataList = new ArrayList<>();

    //省列表
    private List<Province> provinces;

    //市列表
    private List<City> mCitys;

    //县列表
    private List<County> mCountys;

    //选中的省
    private Province mSelectedProvince;

    //选中的城市
    private City mSelectedCity;

    //当前选中的级别
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mTitleText = view.findViewById(R.id.title_city);
        mBtnBack = view.findViewById(R.id.btn_back);
        mListView = view.findViewById(R.id.list_view);

        mAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    mSelectedProvince = provinces.get(position);
                    queryCitys();
                } else if (currentLevel == LEVEL_CITY) {
                    mSelectedCity = mCitys.get(position);
                    queryCountys();
                }
            }
        });

        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTRY) {
                    queryCitys();
                } else if (currentLevel == LEVEL_CITY) {
                    queryCountys();
                }
            }
        });
        queryProvinces();
    }

    /**
     * 查询全国省列表，优先从数据库查询
     */
    private void queryProvinces() {
        mTitleText.setText("中国");
        mBtnBack.setVisibility(View.GONE);
        provinces = DataSupport.findAll(Province.class);
        if (provinces.size() > 0) {
            dataList.clear();
            for (Province province : provinces) {
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china/";
            queryFromServer(address, "province");
        }

    }

    private void queryCitys() {
        mTitleText.setText(mSelectedProvince.getProvinceName());
        mBtnBack.setVisibility(View.VISIBLE);
        mCitys = DataSupport.where("provinceid= ?"
                , String.valueOf(mSelectedProvince.getId())).find(City.class);
        if (mCitys.size() > 0) {
            dataList.clear();
            for (City city :
                    mCitys) {
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    private void queryCountys() {
        mTitleText.setText(mSelectedCity.getCityName());
        mBtnBack.setVisibility(View.VISIBLE);
        mCountys = DataSupport.where("cityid= ?"
                , String.valueOf(mSelectedCity.getId())).find(County.class);
        if (mCountys.size() > 0) {
            dataList.clear();
            for (County county :
                    mCountys) {
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = mSelectedProvince.getProvinceCode();
            int cityCode = mSelectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }


    private void queryFromServer(String address, String type) {
        showProgressDialog();
        HttpUtil.sendHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String reposeText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(reposeText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(reposeText, mSelectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(reposeText, mSelectedCity.getId());
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCitys();
                            } else if ("county".equals(type)) {
                                queryCountys();
                            }
                        }
                    });
                }
            }
        });
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setTitle("正在加载");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }
}
