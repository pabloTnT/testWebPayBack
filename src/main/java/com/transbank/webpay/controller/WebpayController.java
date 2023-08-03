package com.transbank.webpay.controller;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.common.IntegrationCommerceCodes;
import cl.transbank.common.IntegrationType;
import cl.transbank.webpay.common.WebpayOptions;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCommitResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionCreateResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionRefundResponse;
import cl.transbank.webpay.webpayplus.responses.WebpayPlusTransactionStatusResponse;
import com.transbank.webpay.dto.TransactionParameterDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
public class WebpayController extends BaseController{

    private WebpayPlus.Transaction tx;

    public WebpayController(){
        tx = new WebpayPlus.Transaction(new WebpayOptions(IntegrationCommerceCodes.WEBPAY_PLUS, IntegrationApiKeys.WEBPAY, IntegrationType.TEST));
    }

    @GetMapping("/test")
    public String test(){
        return "test response";
    }
    @PostMapping(value = "/webpay_plus/create")
    public WebpayPlusTransactionCreateResponse create(@RequestBody TransactionParameterDto request) {
        String buyOrder = request.getBuy_order();
        String sessionId = request.getSession_id();
        double amount = request.getAmount();
        String returnUrl = request.getReturn_url();

        Map<String, Object> details = new HashMap<>();

        details.put("buyOrder", buyOrder);
        details.put("sessionId", sessionId);
        details.put("amount", amount);
        details.put("returnUrl", returnUrl);
        WebpayPlusTransactionCreateResponse response = new WebpayPlusTransactionCreateResponse();
        try {
            response = tx.create(buyOrder, sessionId, amount, returnUrl);
            details.put("url", response.getUrl());
            details.put("token", response.getToken());

            details.put("resp", toJson(response));
        }
        catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }

        return response;
    }

    @RequestMapping(value = {"/webpay_plus/commit"}, method = { RequestMethod.GET, RequestMethod.POST })
    public ModelAndView commit(@RequestParam("token_ws") String tokenWs, HttpServletRequest request) {
        log.info(String.format("token_ws : %s", tokenWs));

        Map<String, Object> details = new HashMap<>();
        details.put("token_ws", tokenWs);

        try {
            final WebpayPlusTransactionCommitResponse response = tx.commit(tokenWs);
            log.debug(String.format("response : %s", response));
            details.put("amount", response.getAmount());
            details.put("response", response);
            details.put("refund-endpoint", "/webpay_plus/refund");
            details.put("status-endpoint", "/webpay_plus/status");

            details.put("resp", toJson(response));
        }
        catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }
        return new ModelAndView("webpay_plus/commit", "details", details);
    }

    @RequestMapping(value = "/webpay_plus/refund", method = RequestMethod.POST)
    public ModelAndView refund(@RequestParam("token_ws") String tokenWs,
                               @RequestParam("amount") double amount,
                               HttpServletRequest request) {
        log.info(String.format("token_ws : %s | amount : %s", tokenWs, amount));
        Map<String, Object> details = new HashMap<>();
        details.put("token_ws", tokenWs);

        try {
            final WebpayPlusTransactionRefundResponse response = tx.refund(tokenWs, amount);
            details.put("response", response);
            details.put("resp", toJson(response));
        }
        catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }

        return new ModelAndView("webpay_plus/refund", "details", details);
    }
    @RequestMapping(value= "/webpay_plus/status", method = RequestMethod.POST)
    public ModelAndView status(@RequestParam("token_ws") String token){

        Map<String, Object> details = new HashMap<>();
        details.put("token_ws", token);
        try {
            final WebpayPlusTransactionStatusResponse response = tx.status(token);
            details.put("resp", toJson(response));
        }
        catch (Exception e) {
            log.error("ERROR", e);
            details.put("resp", e.getMessage());
        }
        return new ModelAndView("webpay_plus/status", "details", details);
    }
}
