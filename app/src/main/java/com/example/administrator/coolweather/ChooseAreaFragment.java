package com.example.administrator.coolweather;


import android.annotation.TargetApi;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.coolweather.db.City;
import com.example.administrator.coolweather.db.County;
import com.example.administrator.coolweather.db.Province;
import com.example.administrator.coolweather.util.HttpUtil;
import com.example.administrator.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017-4-18.
 */
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listview;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    /*
    省列表
     */
    private List<Province> provinceList;

    //    市
    private List<City> cityList;
    //    县列表
    private List<County> countyList;

    //    选中的省份
    private Province selectedProvince;
    //    选中的城市
    private City selectedCity;
    //    当前选中的级别
    private int currentLevel;




    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_Text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listview = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listview.setAdapter(adapter);//给listview设置一个适配器
        return view;

    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {//如果点击了省
                    selectedProvince = provinceList.get(position);//get访问跟被选择province相关的数据
                    queryCities();//查询省的所有市
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();//查询市的所有县
                } else if (currentLevel==LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    if(getActivity()instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    }else if(getActivity() instanceof WeatherActivity){
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }

            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {//如果点击了县
                    queryCities();  //查询省的所有市
                } else if (currentLevel == LEVEL_CITY) {//如果点击了市
                    queryProvinces();//查询所有省

                }

            }
        });
        queryProvinces();//刚开始遍历全国所有省


    }
    /*
       查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
        */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);//隐藏可视化，并且不占用任何xml空间
        provinceList = DataSupport.findAll(Province.class);//findall方法返回Province所有数据
        if(provinceList.size()>0){
            dataList.clear();
            for(Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();//通知附属的视图基础数据已经改变，视图应该自动刷新。
            listview.setSelection(0);//选中position指定的项目。position从零开始
            currentLevel = LEVEL_PROVINCE;

        }else{
            String address = "http://guolin.tech/api/china/";
            queryFromServer(address,"province");
        }


    }


    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());//点到哪个省就在TEXTVIEW上显示
        backButton.setVisibility(View.VISIBLE);//设置backButton状态是可见的
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        //查询provinceid 为所选中的省 的id
        if (cityList.size() > 0) {
            dataList.clear();//清空这个数组里面的所有元素


            for (City city : cityList) {
                dataList.add(city.getCityName());//遍历，将省份的名字加载到List里面

            }
            adapter.notifyDataSetChanged();//不用刷新Activity通知activity动态刷新listview
            listview.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
        else{
            int provinceCode = selectedProvince.getProvinceCode();//如果没有从数据库读取到数据就
            //组装出一个请求地址调用queryFromServer方法从服务器上查询数据。
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");


        }

    }
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());//点到哪个省就在TEXTVIEW上显示
        backButton.setVisibility(View.VISIBLE);//设置backButton状态是可见的
        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        //查询   为所选中的  的id
        if (countyList.size() > 0) {
            dataList.clear();//清空这个数组里面的所有元素


            for (County county : countyList) {
                dataList.add(county.getCountyName());//遍历，将 的名字加载到List里面

            }
            adapter.notifyDataSetChanged();//不用刷新Activity通知activity动态刷新listview
            listview.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
        else{
            int provinceCode = selectedProvince.getProvinceCode();//如果没有从数据库读取到数据就
            //组装出一个请求地址调用queryFromServer方法从服务器上查询数据。
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode+"/"+cityCode;
            queryFromServer(address, "county");


        }
    }
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
        //该方法向服务器发送请求，响应数据会回调到onResponse方法中

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread（）方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                    //解析处理服务器返回的省级数据，并储存到数据库

                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                    //解析处理
                }else if("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());

                }
                if(result)
                {
                    getActivity().runOnUiThread(new Runnable() {
                        //该方法实现冲子线程切换到主线程
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)) {
                                queryProvinces();
                            }else  if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }



                        }
                    });
                }

            }
        });
    }

    private void closeProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.dismiss();
        }
        progressDialog.show();
    }

    private void showProgressDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }


}

