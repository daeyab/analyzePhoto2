package com.example.analyzephoto

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class PermissionUtil {
    //원하는 권한을 요청하고 있는것는 있다고 이아기해주고 없는 거는 ㅇ권한을 요청

    //권한을 얻기 위해선 액티비티와 리퀘스트 코드와 어떤 궈한을 받고 싶은지 적어줘야함  | 밑에 가변 인수
    fun requestPermission(
        activity: Activity,
        requestCode: Int,
        vararg permissions: String
    ): Boolean {
        var granted = true
        val permissionNeeded = ArrayList<String>()

        //for은 인덱스를 사용, forEach는 인덱스를 사용하지 않음 객체 하나하나만 볼 떄 좋음
        permissions.forEach {
            val permissionCheck =
                ContextCompat.checkSelfPermission(activity, it)//forEach 로 넘어온 하나씩 낱개
            val hasPermission = permissionCheck == PackageManager.PERMISSION_GRANTED
            granted = granted and hasPermission
            if (!hasPermission) {
                permissionNeeded.add(it)
            }
        }
        if (granted)
            return true
        else {
            ActivityCompat.requestPermissions(
                activity,
                permissionNeeded.toTypedArray(),
                requestCode
            )
            return false
        }
    }

    fun permissionGranted(
        requestCode: Int, permissionCode:Int, grantResults:IntArray
    ) : Boolean {
        return requestCode == permissionCode && grantResults.size>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED
    }
}