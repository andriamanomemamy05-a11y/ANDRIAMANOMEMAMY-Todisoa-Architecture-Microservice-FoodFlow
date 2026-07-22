import { Controller, Get, Post, Body, Param } from '@nestjs/common';
import { PaymentsService } from './payments.service';
import { CreatePaymentDto } from './dto/create-payment.dto';

@Controller('payments')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  @Post()
  async create(@Body() dto: CreatePaymentDto) {
    return await this.paymentsService.create(dto);
  }

  @Get('by-order/:orderId/status')
  async getStatusByOrderId(@Param('orderId') orderId: string) {
    const payment = await this.paymentsService.findByOrderId(orderId);
    return payment.status;
  }
}