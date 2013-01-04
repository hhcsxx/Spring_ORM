package net.chuangdie.lhb.bean;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

public class MainTest
{
	public static void main(String[] args)
	{
		ApplicationContext factory = new FileSystemXmlApplicationContext(
				"src/main/resources/hello.xml");

		Shopping shop = (Shopping) factory.getBean("shopping");
		shop.createTable();
		shop.setName("你好啊yayaya");
		shop.setPrice(10.02);
		// shop.save();
		try
		{
			shop.clearOldData();
			List<Object> shops = shop.query(null, null, -1, -1, null);
			for (int i = 0; i < shops.size(); i++)
			{
				Shopping s = (Shopping) shops.get(i);
				System.out.println("id:" + s.getId() + " name:" + s.getName() + " price:"
						+ s.getPrice());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
