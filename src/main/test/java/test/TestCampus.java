package test;

import org.junit.Assert;
import org.junit.Test;

import br.edu.utfpr.dv.siacoes.model.Campus;

public class TestCampus {

	@Test
	public void testAddressEmpty() {
		Campus cam = new Campus();
		cam.setAddress("teste");
		
		Assert.assertEquals("teste", cam.getAddress());

	}
}
