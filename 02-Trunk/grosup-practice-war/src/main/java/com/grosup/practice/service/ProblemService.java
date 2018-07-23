package com.grosup.practice.service;

import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grosup.practice.beans.ProblemBean;
import com.grosup.practice.beans.RecordBean;
import com.grosup.practice.dao.ProblemDao;
import com.grosup.practice.dao.RecordDao;
import com.grosup.practice.dao.StatisticsDao;
import com.grosup.practice.util.GrosupException;
import com.grosup.practice.util.ObjectUtil;

@Service
public class ProblemService {
	
	private static Logger logger = Logger.getLogger(ProblemService.class);
	@Autowired
	private ProblemDao problemDao;
	@Autowired
	private StatisticsDao statisticsDao;
	@Autowired
	private RecordDao recordDao;
	
	/**获得随机一道题
	 * @throws GrosupException */
	public ProblemBean getRandomOne(Map<String, Object> queryParam) throws GrosupException{
		return problemDao.getRandomOne(queryParam);
	}
	
	/**检查是否做对
	 * @throws Exception */
	@Transactional
	public boolean checkAnswer(String problemKey, String answer, int userID, String expression1, String expression2, String expression3) throws GrosupException {
		
		boolean result = true;
		ProblemBean problemBean = problemDao.getProblemByKey(problemKey);
		if (ObjectUtil.isNull(answer) || "".equals(answer)) {
			logger.error("答案不能为空");
			throw new GrosupException(-1, "答案不能为空");
		}
		
		//如果题型是计算题
		if (!"App-Pr-Grade2".equals(problemBean.getKnowledgeKey())) {
			//做题错误
			if (!answer.trim().equals(problemBean.getAnswer())) {
				result = false;
			} 
		} else {//如果是应用题
			result = checkApplication(expression1, expression2, expression3, answer, problemBean);
		}
		//判断做题记录是否存在
		//可以写个初始化加载方法 TODO
		if (!statisticsDao.checkIsExist(userID, problemBean.getKnowledgeKey())) {
			statisticsDao.addOneRecord(userID, problemBean.getKnowledgeKey());
		}
		
		//判断错题表是否存在此题
		boolean checkIsExistInRecord = false;
		
		//判断做题结果更新做题记录及错误结果
		if (!result) {
			RecordBean record = new RecordBean();
			record.setProblemKey(problemKey);
			record.setUserID(userID);
			record.setUserAnswer(answer);
			record.setExpression1(expression1);
			record.setExpression2(expression2);
			record.setExpression3(expression3);
			//更新做题记录并添加到错题集
			//TODO 可以优化为存在即更新
			if (recordDao.checkIsExist(record)) {//如果错题存在则更新，否则添加一条新记录 
				checkIsExistInRecord = true;
				recordDao.updateRecord(record);
			} else {
				recordDao.addRecord(record);
			}
			statisticsDao.updateUserDoneByUnCorrect(userID, problemBean.getKnowledgeKey());
		}
		// 更正错题状态
		if (checkIsExistInRecord) {
			recordDao.correction(userID, problemBean.getProblemKey());
		} 
		statisticsDao.updateUserDoneByCorrect(userID, problemBean.getKnowledgeKey());
		return result;
	}
	
	
	public static boolean checkApplication(String expression1, String expression2, String expression3, String answer, ProblemBean problemBean) {
		if (answer.trim().equals(problemBean.getAnswer()) && expression1.trim().equals(problemBean.getExpression1()) && 
				expression2.trim().equals(problemBean.getExpression2()) && expression3.trim().equals(problemBean.getExpression3())) {
			return true;
		}
		return false;
	}
	
	/**根据题目ID取题目
	 * @throws GrosupException */
	public ProblemBean getProblemByID(String problemKey) throws GrosupException {
		return problemDao.getProblemByKey(problemKey);
	}
}
