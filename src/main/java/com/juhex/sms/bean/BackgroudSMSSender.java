package com.juhex.sms.bean;

import com.juhex.sms.dao.SMSSendDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class BackgroudSMSSender {

    private ExecutorService senderExecutor;

    @Autowired
    private SMSClient smsClient;

    @Autowired
    private SMSSendDAO smsSendDAO;

    @Autowired
    private SMSRespPackageParser smsRespPackageParser;

    private Logger logger;

    @PostConstruct
    public void init(){
        // 初始化两个发送验证码的短息线程足以
        this.senderExecutor = Executors.newFixedThreadPool(2);
        logger = LoggerFactory.getLogger(this.getClass().getCanonicalName());
    }

    public void submitSingleJob(final SMSJob job){

        Runnable run = ()-> {
            String result = smsClient.sendSms(job.getUrl(),job.getAccount(),job.getPassword(),job.getMobile(),job.getContent(),job.getExtno(),job.getRt());
            logger.info("[SMS-RESULT] : {}",result);
            if(!"ERROR".equals(result)){
                // 发送成功入库
                SMSResp resp = smsRespPackageParser.parseSMSResultText(result);
                SMSPackage pack = resp.getList().get(0);
                Long merchantId = job.getMerchantId();
                String msg = job.getContent();
                smsSendDAO.insertIntoMTVCode(merchantId,msg,resp.getStatus(),job.getMobile(),pack.getMid(),pack.getResult());
            }
        };

        senderExecutor.submit(run);
    }

}