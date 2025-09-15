package com.ead.gearup.service;

import com.ead.gearup.dto.response.LoginResponseDTO;
import com.ead.gearup.dto.response.UserResponseDTO;
import com.ead.gearup.dto.user.UserCreateDTO;
import com.ead.gearup.dto.user.UserLoginDTO;

public interface UserService {

    public UserResponseDTO createUser(UserCreateDTO userCreateDTO);

    public LoginResponseDTO verifyUser(UserLoginDTO userlLoginDTO);
}
