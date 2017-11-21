package com.ecc.spring.service;

import java.io.Serializable;
import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.Validator;
import org.springframework.validation.Errors;

import com.ecc.spring.dao.PermissionDao;
import com.ecc.spring.dao.UserDao;
import com.ecc.spring.dao.PermissionDao;
import com.ecc.spring.dto.UserDTO;
import com.ecc.spring.model.Permission;
import com.ecc.spring.model.User;
import com.ecc.spring.util.AssemblerUtils;
import com.ecc.spring.util.ValidationUtils;
import com.ecc.spring.util.ValidationException;

@Service
public class UserService extends AbstractService<User, UserDTO> implements Validator, UserDetailsService {
	private static final Integer MAX_CHARACTERS = 255;

	private final UserDao userDao;
	private Map<String, Permission> PERMISSIONS_HASH;

	@Autowired
	private PermissionDao permissionDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("#{permissionDao.list()}")
	private List<Permission> PERMISSIONS;

	public UserService(UserDao userDao) {
		super(userDao);
		this.userDao = userDao;
	}

	@Override
	public boolean supports(Class clazz) {
    return clazz.isAssignableFrom(UserDTO.class);
  }

  @Override
  public void validate(Object command, Errors errors) {
  	UserDTO user = (UserDTO) command;
		ValidationUtils.testNotEmpty(user.getUsername(), "username", errors, "localize:user.data.column.username");
		ValidationUtils.testMaxLength(user.getUsername(), "username", errors, MAX_CHARACTERS, "localize:user.data.column.username");
  }

	@Override
	public Serializable create(UserDTO user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		return super.create(user);
	}

	@Override
	public void update(UserDTO user) {
		UserDTO originalUser = get(user.getId());
		if (!StringUtils.isEmpty(user.getPassword())) {
			user.setPassword(passwordEncoder.encode(user.getPassword()));
		}
		else {
			user.setPassword(originalUser.getPassword());
		}
		super.update(user);
	}

	@PostConstruct
	public void init() {
		PERMISSIONS_HASH = permissionDao.list().stream().collect(Collectors.toMap(t -> t.getName(), Function.identity()));
	}
	
	public List<UserDTO> list() {
		return AssemblerUtils.asList(userDao.list(), this::createDTO);
	}

	@Override
	public UserDTO createDTO(User model) {
		if (model == null) {
			return null;
		}
		UserDTO dto = new UserDTO();
		dto.setId(model.getId());
		dto.setUsername(model.getUsername());
		dto.setPassword(model.getPassword());
		dto.setPermissions(model.getPermissions().stream()
			.map(t -> t.getName())
			.collect(Collectors.toList()));
		return dto;
	}

	@Override 
	public User createModel(UserDTO dto) {
		if (dto == null) {
			return null;
		}
		User model = new User();
		model.setId(dto.getId());
		model.setUsername(dto.getUsername());
		model.setPassword(dto.getPassword());
		model.setPermissions(dto.getPermissions().stream()
			.map(t -> PERMISSIONS_HASH.get(t))
			.collect(Collectors.toSet()));
		return model;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		User user = userDao.get(username);

		return user == null? null: 
			new org.springframework.security.core.userdetails.User(username, user.getPassword(), getAuthorities(user.getPermissions()));
	}

	@Override
	protected RuntimeException onCreateFailure(User user, RuntimeException cause) {
		user.setId(null);
		return onUpdateFailure(user, cause);
	}

	@Override
	protected RuntimeException onUpdateFailure(User user, RuntimeException cause) {
		if (cause instanceof DataIntegrityViolationException) {
			return new ValidationException("user.validation.message.duplicateEntry", this.createDTO(user), user.getUsername());
		}
		return super.onUpdateFailure(user, cause);
	}

	@Override
	protected RuntimeException onGetFailure(Integer id, RuntimeException cause) {
		if (cause instanceof DataRetrievalFailureException) {
			return new ValidationException("user.validation.message.notFound", new UserDTO(), id);		
		}
		return super.onGetFailure(id, cause);
	}

	private Collection<? extends GrantedAuthority> getAuthorities(Collection<Permission> permissions) {
		if (permissions.contains(new Permission("ROLE_ADMIN"))) {
			permissions = PERMISSIONS;
		}

		return permissions.stream()
			.map(t -> new SimpleGrantedAuthority(t.getName()))
			.collect(Collectors.toList());
	}
}