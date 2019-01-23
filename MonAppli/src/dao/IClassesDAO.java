package dao;

import java.util.List;

public interface IClassesDAO<T>
{
	public List<T> select(String req);
}