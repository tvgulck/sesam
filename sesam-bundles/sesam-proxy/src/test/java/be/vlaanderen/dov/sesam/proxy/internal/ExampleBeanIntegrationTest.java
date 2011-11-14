/*
 * Copyright 2011 Vlaams Gewest
 *
 * This file is part of SESAM, the Service Endpoint Security And Monitoring framework.
 *
 * SESAM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SESAM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SESAM.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.vlaanderen.sesam.proxy.internal;

import org.junit.Ignore;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;

/**
 * Integration test the bundle locally (outside of OSGi).
 * Use AbstractOsgiTests and a separate integration test project
 * for testing inside of OSGi.
 */
@Ignore
public class ExampleBeanIntegrationTest extends AbstractDependencyInjectionSpringContextTests
{
//    private ExampleBean myBean;
//
//    protected String[] getConfigLocations()
//    {
//        return new String[] { "META-INF/spring/bundle-context.xml" };
//    }
//
//    public void setBean( ExampleBean bean )
//    {
//        this.myBean = bean;
//    }
//
//    public void testBeanIsABean()
//    {
//        assertTrue( this.myBean.isABean() );
//    }
}
