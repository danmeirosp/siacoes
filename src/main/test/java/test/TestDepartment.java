package test;

import org.junit.Assert;
import org.junit.Test;

import br.edu.utfpr.dv.siacoes.model.Department;

public class TestDepartment {

	@Test
	public void testNameEquals() {
		Department dep = new Department();
		dep.setName("teste1");
		
		Assert.assertEquals("teste1", dep.getName());
		
	}
}