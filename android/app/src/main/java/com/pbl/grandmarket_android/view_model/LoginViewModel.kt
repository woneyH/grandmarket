package com.pbl.grandmarket_android.view_model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pbl.grandmarket_android.UserRole
import com.pbl.grandmarket_android.util.Resource
import com.pbl.grandmarket_android.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _loginStatus = MutableLiveData<Resource<String>>()
    val loginStatus: LiveData<Resource<String>> = _loginStatus

    private val _loginRole = MutableLiveData<UserRole>()
    val loginRole: LiveData<UserRole> = _loginRole

    fun performKakaoLogin(accessToken: String, role: UserRole) {
        _loginRole.value = role
        _loginStatus.value = Resource.Loading

        viewModelScope.launch {
            try {
                val response = repository.kakaoLogin(accessToken, role)
                if(response.isSuccessful) {
                    val jwt = response.body()
                    if(!jwt.isNullOrEmpty()) {
                        _loginStatus.value = Resource.Success(jwt)
                    }else {
                        _loginStatus.value = Resource.Error("JWT 응답이 비어있습니다.")
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
