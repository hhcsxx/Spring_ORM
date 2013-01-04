package net.chuangdie.lhb.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

public abstract class BasicBean
{
	private String tableName;
	private String primaryKey;

	private Object __obj;// bean的具体类型

	private JdbcTemplate jdbcTemplate;

	public List<Object> query(String _select, String _whereSql, int _page, int _pageSize,
			String _orderBy) throws InstantiationException, IllegalAccessException, SQLException
	{
		StringBuffer sql = new StringBuffer();
		if (_select == null)
		{
			sql.append("select * from ");
		} else
		{
			sql.append("select " + _select + " from ");
		}
		sql.append(tableName);
		if (null == _whereSql)
		{
			sql.append(object2QuerySql(__obj));
		} else
		{
			sql.append(_whereSql);
		}
		if (_orderBy != null)
		{
			sql.append(" order by " + _orderBy);
		}
		if (_page != -1)
		{
			sql.append(" limit " + _page * _pageSize + ", " + _pageSize);
		}
		// List<Object> cacheResult = dbCache.getQuery(sql.toString());
		// if (cacheResult != null)
		// {
		// return cacheResult;
		// }
		List<Object> rsList = jdbcTemplate.query(sql.toString(), new RowMapper<Object>()
		{
			public Object mapRow(ResultSet rs, int rowNum) throws SQLException
			{
				ResultSetMetaData rsmd = rs.getMetaData();
				int columnCount = rsmd.getColumnCount();
				Object obj = null;
				try
				{
					obj = __obj.getClass().newInstance();
				} catch (Exception e)
				{
					// 丢掉一个数据，原因这个类不能被初始化
				}
				if (obj != null)
				{
					for (int j = 1; j <= columnCount; j++)
					{
						obj = setter(obj, rsmd.getColumnName(j), rs.getObject(j));
					}
				}
				return obj;
			}
		});
		return rsList;
	}

	public int save()
	{
		StringBuffer sql = new StringBuffer();
		sql.append("insert into " + this.tableName);
		if (__obj != null)
		{
			Class<?> c = __obj.getClass();
			Field[] field = c.getDeclaredFields();
			boolean flag = true;
			// 将输入的内容补完
			for (int i = 0; i < field.length; i++)
			{
				Object value = getter(__obj, field[i].getName());
				if (value != null && !value.equals(""))
				{
					if (flag)
					{
						sql.append(" (");
						flag = false;
					} else
					{
						sql.append(" , ");
					}
					sql.append(field[i].getName());
				}
			}
			sql.append(" )");
			// 将values中的值补全
			flag = true;
			for (int i = 0; i < field.length; i++)
			{
				Object value = getter(__obj, field[i].getName());
				if (value != null && !value.equals(""))
				{
					if (flag)
					{
						sql.append(" values (");
						flag = false;
					} else
					{
						sql.append(" , ");
					}
					if (value instanceof String)
					{
						sql.append("'" + tranChar((String) value) + "'");
					} else
					{
						sql.append(value);
					}
				}
			}
			sql.append(" ) ");
			// if (CommCons.isDebug)
			// {
			// System.out.println(sql.toString());
			// }
			return jdbcTemplate.update(sql.toString());
		}
		return -1;
	}

	/**
	 * 如果不传入值，始终用主键为替换条件
	 * 
	 * @return
	 */
	public int update(String _whereSql)
	{
		// dbCache.clearQuery(_set);
		StringBuffer sql = new StringBuffer();
		// 用where的类型名为主
		sql.append("update " + this.tableName + " set ");
		sql.append(object2UpdateSet(__obj));
		if (null == _whereSql)
		{
			sql.append(" where " + this.primaryKey + " = " + getter(__obj, this.primaryKey));
		} else
		{
			sql.append(_whereSql);
		}
		// if (CommCons.isDebug)
		// {
		// System.out.println(sql.toString());
		// }
		return jdbcTemplate.update(sql.toString());
	}

	public int del(String _whereSql)
	{
		// dbCache.clearQuery(_o);
		StringBuffer sql = new StringBuffer();
		sql.append("delete from " + this.tableName + " ");
		if (null == _whereSql)
		{
			sql.append(object2QuerySql(__obj));
		} else
		{
			sql.append(_whereSql);
		}
		// if (CommCons.isDebug)
		// {
		// System.out.println(sql.toString());
		// }

		return jdbcTemplate.update(sql.toString());
	}

	public void executeQuery(String sql)
	{

	}

	public int executeUpdate(String sql)
	{
		return jdbcTemplate.update(sql);
	}

	/**
	 * 将一个obj反射出来类型
	 * 
	 * @param o
	 * @return
	 */
	private String object2QuerySql(Object _obj)
	{
		StringBuffer result = new StringBuffer();
		if (_obj != null)
		{
			if (_obj instanceof String)
			{
				result.append(_obj);
			} else
			{
				Class<?> c = _obj.getClass();
				Field[] field = c.getDeclaredFields();
				boolean flag = true;
				for (int i = 0; i < field.length; i++)
				{
					Object value = getter(_obj, field[i].getName());
					if (value != null && !value.equals(""))
					{
						if (flag)
						{
							result.append(" where ");
							flag = false;
						} else
						{
							result.append(" and ");
						}
						if (value instanceof String)
						{
							result.append(field[i].getName() + " = " + "'"
									+ tranChar((String) value) + "'");
						} else
						{
							result.append(field[i].getName() + " = " + value);
						}
					}
				}
			}
		}
		return result.toString();
	}

	private static Object getter(Object o, String att)
	{
		try
		{
			Method method = o.getClass().getMethod(
					"get" + att.substring(0, 1).toUpperCase() + att.substring(1).toLowerCase());
			if (null == method.invoke(o))
			{
				return null;
			}
			return method.invoke(o);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 数据写入数据库时 将单引号变为两个单引号
	 * 
	 * @param value
	 * @return
	 */
	private static String tranChar(String value)
	{
		return value.replaceAll("'", "''").replaceAll("\\\\", "\\\\\\\\");
	}

	/**
	 * 类名+属性名+属性的值 就可以为这个类赋值了
	 * 
	 * @param o
	 * @param att
	 * @param value
	 * @return
	 */
	private static Object setter(Object o, String att, Object value)
	{
		try
		{
			// 通过名字判断这个属性的类型
			Method method = o.getClass().getMethod(
					"set" + att.substring(0, 1).toUpperCase() + att.substring(1).toLowerCase(),
					o.getClass().getDeclaredField(att.toLowerCase()).getType());
			method.invoke(o, value);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return o;
	}

	private String object2UpdateSet(Object o)
	{
		StringBuffer result = new StringBuffer();
		if (o != null)
		{
			if (o.getClass().equals(String.class))
			{
				result.append(o);
			} else
			{
				Class<?> setC = o.getClass();
				Field[] fieldC = setC.getDeclaredFields();
				boolean flag = true;
				for (int i = 0; i < fieldC.length; i++)
				{
					Object value = getter(o, fieldC[i].getName());
					if (value != null && !value.equals(""))
					{
						if (flag)
						{
							flag = false;
						} else
						{
							result.append(" , ");
						}
						if (value instanceof String)
						{
							result.append(fieldC[i].getName() + " = " + "'"
									+ tranChar((String) value) + "'");
						} else
						{
							result.append(fieldC[i].getName() + " = " + value);
						}

					}
				}
			}
		}
		return result.toString();
	}

	/**
	 * 将这个类的属性全部重置
	 */
	public void clearOldData()
	{
		Field[] fieldC = __obj.getClass().getDeclaredFields();
		for (int i = 0; i < fieldC.length; i++)
		{
			setter(__obj, fieldC[i].getName(), null);
		}
	}

	public String getTableName()
	{
		return tableName;
	}

	public void setTableName(String tableName)
	{
		this.tableName = tableName;
	}

	public String getPrimaryKey()
	{
		return primaryKey;
	}

	public void setPrimaryKey(String primaryKey)
	{
		this.primaryKey = primaryKey;
	}

	public Object getObj()
	{
		return __obj;
	}

	public void setObj(Object obj)
	{
		this.__obj = obj;
	}

	public void setJdbcTemplate(JdbcTemplate jdbcTemplate)
	{
		this.jdbcTemplate = jdbcTemplate;
	}

}
