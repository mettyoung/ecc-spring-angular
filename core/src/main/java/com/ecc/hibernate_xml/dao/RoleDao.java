package com.ecc.hibernate_xml.dao;

import java.util.List;

import org.hibernate.Session;

import com.ecc.hibernate_xml.model.Role;
import com.ecc.hibernate_xml.util.TransactionScope;
import com.ecc.hibernate_xml.util.HibernateUtility;

public class RoleDao {

	public List<Role> listRoles() {
		Session session = HibernateUtility.getSessionFactory().openSession();
		List<Role> roles = session.createQuery("FROM Role ORDER BY id").list();
		session.close();
		return roles;
	}

	public void createRole(Role newRole) throws DaoException {
		try {
			TransactionScope.executeTransaction(session -> session.save(newRole));			
		}
		catch (Exception exception) {
			throw new DaoException(exception);
		}
	}

	public void updateRole(Integer roleId, Role newRole) throws DaoException {
		try {
			TransactionScope.executeTransaction(session -> {
				newRole.setId(roleId);
				session.update(newRole);
			});
		}
		catch (Exception exception) {
			throw new DaoException(exception);
		}
	}

	public void deleteRole(Integer roleId) throws DaoException {
		try {
			TransactionScope.executeTransaction(session -> 
				session.delete(session.get(Role.class, roleId)));
		}
		catch (Exception exception) {
			throw new DaoException(exception);
		}
	}
}