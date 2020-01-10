package com.example.analyzephoto

import android.content.pm.PackageManager
import android.content.pm.Signature
import com.google.common.io.BaseEncoding
import java.lang.Exception
import java.security.MessageDigest

class PackageManagerUtil{
    fun getSignature(pm:PackageManager, packageName:String):String?{
        try {
            val packageInfo=pm.getPackageInfo(packageName,PackageManager.GET_SIGNATURES)
            return if(packageInfo==null || packageInfo.signatures==null || packageInfo.signatures.size==0 || packageInfo.signatures[0]==null){ null }
            else{
                    signatureDigest(packageInfo.signatures[0])
            }
        }
        catch (e:java.lang.Exception){
            return null

        }
    }

    private fun signatureDigest(sig:Signature):String?{
        val signature=sig.toByteArray()
        try {
            val md=MessageDigest.getInstance("SHA1")
            val digest=md.digest(signature)
            return BaseEncoding.base16().lowerCase().encode(digest)
        }
        catch (e :Exception){
            return null
        }
    }
}