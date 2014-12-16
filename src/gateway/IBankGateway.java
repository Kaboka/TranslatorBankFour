/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gateway;

import webservice.LoanRequest;

/**
 *
 * @author Kasper
 */
public interface IBankGateway {
    public void contactBank(LoanRequest request, String correlationId);
}
