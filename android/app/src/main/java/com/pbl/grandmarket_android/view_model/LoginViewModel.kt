package com.pbl.grandmarket_android.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbl.grandmarket_android.network.KakaoLoginResponse
import com.pbl.grandmarket_android.util.Resource
import com.pbl.grandmarket_android.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _loginStatus = MutableLiveData<Resource<KakaoLoginResponse>>()
    val loginStatus: LiveData<Resource<KakaoLoginResponse>> = _loginStatus


    fun performKakaoLogin(accessToken: String) {
        _loginStatus.value = Resource.Loading

        viewModelScope.launch {
            try {
                val response = repository.kakaoLogin(accessToken)

                if(response.isSuccessful) {
                    response.body()?.let {
                        _loginStatus.value = Resource.Success(it)
                    } ?: run {
                        _loginStatus.value = Resource.Error("응답 데이터가 비어있습니다.")
                    }
                }else {
                    _loginStatus.value = Resource.Error("로그인 실패: ${response.code()}")
                }
            }catch (e: Exception) {
                _loginStatus.value = Resource.Error(e.message?: "알 수 없는 오류가 발생함")
            }
        }
    }
}
