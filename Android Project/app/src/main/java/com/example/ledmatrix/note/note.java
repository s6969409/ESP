package com.example.ledmatrix.note;

public class note {
}

/*
DeviceListActivity //TODO 做檢查名稱
OnLineActivity //TODO 封裝一個類含有Online的module...

AP熱點找一下,看一下
https://www.itread01.com/content/1548925387.html

系統設定介面
https://www.itread01.com/qhkhkii.html

自訂View 了解一下?
https://blog.csdn.net/vipzjyno1/article/details/23696537

自訂一個ScrollView 限制 最大高度
https://www.jianshu.com/p/57e7ebea91f4

LedMatrixActivity設定可加入null(代表改燈不做控制,存檔時不輸該資料?,但是讀檔要處理一下如何判斷null資料)

arduino 想一下如何處理連線問題,當client連上,如何邊監聽clientRequest邊做Led程式,
目前已先在已連線地方加入Led程式處理Cycle,想一下是否有更好的方式


已完成 參考MainProgramActivity.java
以下改一改
LedMatrixActivity
ProgramActivity
ProgramBlockEditActivity
FileEditActivity


LED設定資料格式:
    單一LED狀態格式陣列:num,RGB

deviceInfo資料格式:
    types:ledMatrix,...//逗號分隔
    ledMatrix:data//
        data:數量//

想一下{"LED設定資料格式","用戶程式"},
    是以某個資料夾內所有檔案為LED設定檔-->這個比較不麻煩
        資料夾結構:
            deviceConfig:
                apSetting
                deviceInfo
                mainProgram
            resource:
                LEDs: 想一下設定led的方法 參考OnLineTemp的setBitmapShader控制顏色方法 建GridView的List資料
                Prograrm:
    想一下副程式自己CALL自己或其他方式 產生無限迴圈的問題 如何防範

在DeviceFileEditActivity右上角選單加入格式化設備選項,用來初始化設備部分資料夾?

arduino 主程式整理一下,另外建檔案整理???


DeviceTempActivity加入Client 考慮順便加入訊號測試~ 目前已不使用

RecyclerView 子項目改用CardView!? 沒事考慮全改回ListView

新增Dialog 想辦法用setView 優化?

arduino 好像無法刪除資料夾
    sdFat.remove(fileName.c_str());-->刪除文件
    sdFat.rmdir(fileName.c_str());-->刪除目錄(但該子目錄文件必須為空)
    以上兩個方法刪除失敗 回傳false
    做個回傳失敗的字串
    刪除整個資料夾根目錄 要想一下...已在Arduino 端解決

<urgent>收到Server錯誤的回饋 想一下要如何處理

<No urgent>Tree ListView 最後一項顯示Server回傳文字? 找一下圖片 在TreeViewAdapter.updateDataFromDevice()修改

<No urgent>取得設備所有檔案時,Element的id 目前由for 0順序產生 應該是沒什麼問題,考慮產生唯一性?

<?>APP外部儲存區分兩個path 1個存APP 1個存設備備檔? 在FileProcess做路徑字串處理的方法

<urgent>寫資料到設備時 抓手機時間 寫入資料//這裡需配合arduino


取得所有文件的範例字串
{"commandMode":"getFileList","data":"tt.txt,123.txt,193.txt,423.txt,folder<<tt1.txt,333.txt,tt2.txt>","filePath":""}<LF>
取得檔案內容的範例字串
{"commandMode":"readFile","data":"datataaattttttttt","filePath":"exampleTemp.txt"}<LF>
寫入檔案內容的範例字串
{"commandMode":"writeFile","data":"Write OKOK!","filePath":"exampleTemp.txt"}<LF>
移除檔案的範例字串
{"commandMode":"removeFile","data":"Remove -- OKOK!","filePath":"exampleTemp.txt"}<LF>
 */

/*
create Led file process:::::::::::::::::::::::::::::::::::::
App request:
{"commandMode":"readFile","data":"","filePath":"deviceConfig/deviceInfo.txt"}
device feedback:
{"commandMode":"readFile","data":"{\"types\":[\"ledMatrix\"],\"ledMatrix\":{\"ledNums\":8}}","filePath":"deviceConfig/deviceInfo.txt"}

App click "save" button
App request:
{"commandMode":"writeFile","data":"[{\"colorB\":0,\"colorG\":0,\"colorR\":253,\"num\":1},{\"colorB\":0,\"colorG\":0,\"colorR\":0,\"num\":2},{\"colorB\":0,\"colorG\":0,\"colorR\":253,\"num\":3},{\"colorB\":0,\"colorG\":0,\"colorR\":0,\"num\":4},{\"colorB\":0,\"colorG\":0,\"colorR\":0,\"num\":5},{\"colorB\":0,\"colorG\":0,\"colorR\":0,\"num\":6},{\"colorB\":0,\"colorG\":0,\"colorR\":253,\"num\":7},{\"colorB\":0,\"colorG\":0,\"colorR\":253,\"num\":8}]","filePath":"resource/LEDs/newled.txt"}
device feedback:
{"commandMode":"writeFile","data":true,"filePath":"resource/LEDs/newled.txt"}
*/