package org.c4marathon.assignment.user.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.c4marathon.assignment.domain.user.dto.LoginRequestDto;
import org.c4marathon.assignment.domain.user.dto.LoginResponseDto;
import org.c4marathon.assignment.domain.user.dto.UserDto;
import org.c4marathon.assignment.domain.user.entity.LoginResponseMsg;
import org.c4marathon.assignment.domain.user.entity.User;
import org.c4marathon.assignment.domain.user.exception.UserException;
import org.c4marathon.assignment.domain.user.repository.UserRepository;
import org.c4marathon.assignment.domain.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) //
public class UserServiceTest {
	@Mock
	private UserRepository userRepository;
	@InjectMocks
	private UserService userService;
	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private HttpServletRequest httpServletRequest;

	@Mock
	private HttpSession httpSession;

	@AfterEach
	void tearDown() {
		userRepository.deleteAll();
	}

	@BeforeEach
	void setUp() {
		// Mocking HttpSession and HttpServletRequest
		given(httpServletRequest.getSession(true)).willReturn(httpSession);
		given(httpServletRequest.getSession()).willReturn(httpSession);
	}

	@DisplayName("회원가입 시 회원 정보 저장")
	@Test
	void join() {
		//given
		UserDto userDto = new UserDto("010-8337-6023", "조아빈", "20000604", "pw123");
		given(userRepository.save(any(User.class))).willReturn(User.builder().userPhone("010-8337-6023").build());

		//when
		userService.join(userDto);

		//then
		verify(userRepository).save(any(User.class));
	}

	@DisplayName("로그인 시 일치하는 정보가 있다면 로그인은 성공하고 세션이 등록된다.")
	@Test
	void login() {
		// given
		LoginRequestDto loginRequestDto = new LoginRequestDto("010-8337-6023", "pw123");
		User mockUser = User.builder()
			.userPhone("010-8337-6023")
			.userPassword(passwordEncoder.encode("pw123"))
			.build();

		given(userRepository.findByUserPhone(anyString())).willReturn(Optional.of(mockUser));
		given(passwordEncoder.matches(any(), any())).willReturn(Boolean.TRUE);

		given(httpServletRequest.getSession()).willReturn(httpSession);

		// when
		LoginResponseDto responseDto = userService.login(loginRequestDto, httpServletRequest);

		// then
		assertThat(responseDto.responseMsg()).isEqualTo(LoginResponseMsg.SUCCESS.getResponseMsg());
		verify(httpSession).setAttribute("userId", mockUser.getUserId());
		verify(httpSession).setMaxInactiveInterval(1800);
		System.out.println(responseDto.responseMsg());
	}

	@DisplayName("로그인 시 일치하는 전화번호가 없다면 로그인 실패 예외가 발생한다.")
	@Test
	void loginUnvalidUserPhone(){
		//given
		LoginRequestDto loginRequestDto = new LoginRequestDto("010-8337-6024", "pw123");
		given(userRepository.findByUserPhone(anyString())).willReturn(Optional.empty());

		//when //then
		given(httpServletRequest.getSession()).willReturn(httpSession);
		assertThrows(UserException.class, () -> userService.login(loginRequestDto, httpServletRequest));
	}

	@DisplayName("로그인 시 비밀번호가 일치하지 않다면 로그인 실패 예외가 발생한다.")
	@Test
	void loginUnvalidUserPassword() {
		//given
		LoginRequestDto loginRequestDto = new LoginRequestDto("010-8337-6023", "pw111");
		User mockUser = User.builder()
			.userPhone("010-8337-6023")
			.userPassword(passwordEncoder.encode("pw123"))
			.build();

		given(userRepository.findByUserPhone(anyString())).willReturn(Optional.of(mockUser));
		given(passwordEncoder.matches(any(), any())).willReturn(Boolean.FALSE);
		given(httpServletRequest.getSession()).willReturn(httpSession);

		// when //then
		assertThrows(UserException.class, () -> userService.login(loginRequestDto, httpServletRequest));
	}
}
