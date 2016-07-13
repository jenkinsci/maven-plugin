package org.foobar.test;

import static org.junit.Assert.assertEquals;

import org.foobar.Foobar;
import org.junit.Test;

public class FoobarTest
{

  @Test
  public void testGetName()
  {
    assertEquals("foobar", new Foobar().getName());
  }

}
