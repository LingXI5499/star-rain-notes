-- ONLINE projects may use either a real demo URL or a published static prototype.
-- Cross-table availability is enforced by PortfolioService at publish/update time.
ALTER TABLE portfolio_project DROP CHECK ck_portfolio_online_demo;
